/*
 * Copyright 2019-2024 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cloudnetservice.modules.cloudflare.impl;

import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.modules.cloudflare.config.CloudflareConfiguration;
import eu.cloudnetservice.modules.cloudflare.config.CloudflareConfigurationEntry;
import eu.cloudnetservice.modules.cloudflare.config.CloudflareGroupConfiguration;
import eu.cloudnetservice.modules.cloudflare.impl.cloudflare.CloudFlareRecordManager;
import eu.cloudnetservice.modules.cloudflare.impl.cloudflare.DnsRecordDetail;
import eu.cloudnetservice.modules.cloudflare.impl.dns.DnsRecord;
import eu.cloudnetservice.modules.cloudflare.impl.dns.DnsType;
import eu.cloudnetservice.modules.cloudflare.impl.listener.CloudflareServiceStateListener;
import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.impl.util.NetworkUtil;
import eu.cloudnetservice.utils.base.StringUtil;
import io.vavr.Tuple2;
import jakarta.inject.Singleton;
import java.net.Inet6Address;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class CloudNetCloudflareModule extends DriverModule {

  private static final UUID NODE_RECORDS_ID = UUID.randomUUID();
  private static final Logger LOGGER = LoggerFactory.getLogger(CloudNetCloudflareModule.class);

  private CloudflareConfiguration cloudflareConfiguration;

  @ModuleTask(lifecycle = ModuleLifeCycle.LOADED)
  public void convertConfiguration() {
    var config = this.readConfig(DocumentFactory.json()).readDocument("config");
    if (!config.empty()) {
      this.writeConfig(Document.newJsonDocument().appendTree(config));
    }
  }

  @ModuleTask(order = 127, lifecycle = ModuleLifeCycle.STARTED)
  public void loadConfiguration() {
    var config = this.readConfig(
      CloudflareConfiguration.class,
      () -> new CloudflareConfiguration(Lists.newArrayList(new CloudflareConfigurationEntry(
        false,
        CloudflareConfigurationEntry.AuthenticationMethod.GLOBAL_KEY,
        StringUtil.generateRandomString(7),
        NetworkUtil.localAddress(),
        "user@example.com",
        "api_token_string",
        "zoneId",
        "example.com",
        Lists.newArrayList(new CloudflareGroupConfiguration("Proxy", "@", 1, 1))))),
      DocumentFactory.json());
    this.updateConfiguration(config);
  }

  @ModuleTask(order = 126, lifecycle = ModuleLifeCycle.STARTED)
  public void createNodeRecordsWhenNeeded(@NonNull CloudFlareRecordManager recordManager) {
    for (var entry : this.cloudflareConfiguration.entries()) {
      if (entry.enabled()) {
        // collect the host information, continue if invalid
        var hostInformation = this.parseAddressOfEntry(entry);
        if (hostInformation == null) {
          continue;
        }

        // list all records of the entry and check if we need to create a dns record
        var expectedName = String.format("%s.%s", entry.entryName(), entry.domainName());
        recordManager.listRecords(entry).thenAccept(records -> {
          var existingRecord = records.stream()
            .filter(record -> record.type().equals(hostInformation._1().name())
              && record.name().equalsIgnoreCase(expectedName) && record.content().equals(hostInformation._2()))
            .findFirst()
            .orElse(null);

          // check if the record exists or create a new record
          if (existingRecord == null) {
            recordManager.createRecord(NODE_RECORDS_ID, entry, new DnsRecord(
              hostInformation._1(),
              expectedName,
              hostInformation._2(),
              1,
              false,
              Document.newJsonDocument()));
          } else {
            // mark the record as created
            LOGGER.debug("Skipping creation of record for {} because the record {} exists", entry, existingRecord);
            recordManager.trackedRecords().put(
              NODE_RECORDS_ID,
              new DnsRecordDetail(existingRecord.id(), existingRecord, entry));
          }
        });
      }
    }
  }

  @ModuleTask(order = 125, lifecycle = ModuleLifeCycle.STARTED)
  public void finishStartup(@NonNull EventManager eventManager) {
    eventManager.registerListener(CloudflareServiceStateListener.class);
  }

  @ModuleTask(lifecycle = ModuleLifeCycle.RELOADING)
  public void handleReload(
    @NonNull @Service I18n i18n,
    @NonNull CloudFlareRecordManager recordManager,
    @NonNull Configuration nodeConfig
  ) {
    // store the old entries for later comparison
    var oldEntries = this.cloudflareConfiguration.entries();

    // re-load the configuration and get the new entries from it
    this.loadConfiguration();
    var newEntries = this.cloudflareConfiguration.entries();

    // get all entries which are tracked & the entries created for all nodes
    var trackedEntries = recordManager.trackedRecords();
    var nodeRecordEntries = trackedEntries.get(NODE_RECORDS_ID);

    // find all newly added records - while we could also remove all old records, this could result in unexpected issues
    // like proxy SRV records pointing to a deleted record
    var stillExistingEntries = newEntries.stream()
      .filter(CloudflareConfigurationEntry::enabled)
      .flatMap(entry -> oldEntries.stream()
        .filter(old -> CloudflareConfigurationEntry.mightEqual(old, entry))
        .map(oldEntry -> new Tuple2<>(entry, oldEntry)))
      .toList();

    // find all entries which are present in the old and new configuration file
    // without comparing the group-based record creation configurations
    stillExistingEntries.stream()
      .filter(Predicate.not(pair -> {
        var newEntry = pair._1();
        var oldEntry = pair._2();

        // compare the entries if they might equal
        return Objects.equals(newEntry.authenticationMethod(), oldEntry.authenticationMethod())
          && Objects.equals(newEntry.hostAddress(), oldEntry.hostAddress())
          && Objects.equals(newEntry.email(), oldEntry.email())
          && Objects.equals(newEntry.apiToken(), oldEntry.apiToken())
          && Objects.equals(newEntry.zoneId(), oldEntry.zoneId())
          && Objects.equals(newEntry.domainName(), oldEntry.domainName());
      }))
      .map(Tuple2::_1)
      .map(newConfigEntry -> nodeRecordEntries.stream()
        .filter(entry -> CloudflareConfigurationEntry.mightEqual(entry.configurationEntry(), newConfigEntry))
        .findFirst()
        .map(detail -> new Tuple2<>(newConfigEntry, detail))
        .orElseGet(() -> new Tuple2<>(newConfigEntry, null)))
      .forEach(pair -> {
        // try to build a dns record for the entry
        var record = this.buildRecord(pair._1());
        if (record == null) {
          return;
        }

        // patch or create a new record based on the information we've collected
        CompletableFuture<DnsRecordDetail> future;
        if (pair._2() == null) {
          // no previous record found, create a new one
          future = recordManager.createRecord(NODE_RECORDS_ID, pair._1(), record);
        } else {
          // previous record found, remove the tracked one & patch the existing one
          trackedEntries.remove(NODE_RECORDS_ID, pair._2());
          future = recordManager.patchRecord(NODE_RECORDS_ID, pair._2(), record);
        }

        // add a listener to the future to print out a nice message
        future.thenAccept(detail -> {
          // check if the record was created
          if (detail != null) {
            LOGGER.info(i18n.translate(
              "module-cloudflare-create-dns-record-for-service",
              pair._1().domainName(),
              nodeConfig.identity().uniqueId(),
              detail.id()));
          }
        });
      });

    // filter out all newly added entries and create records for them
    var addedEntries = newEntries.stream()
      .filter(entry -> !oldEntries.contains(entry) && stillExistingEntries.stream()
        .noneMatch(pair -> Objects.equals(pair._2().entryName(), entry.entryName())))
      .toList();
    this.createRecordsForEntries(i18n, nodeConfig, recordManager, addedEntries);
  }

  @ModuleTask(order = 64, lifecycle = ModuleLifeCycle.STOPPED)
  public void removeAllServiceRecords(@NonNull CloudFlareRecordManager recordManager) {
    var deletionFutures = recordManager.trackedRecords().entries().stream()
      .filter(trackedRecordEntry -> {
        // check if the entry is a record for our node
        if (trackedRecordEntry.getKey().equals(NODE_RECORDS_ID)) {
          // check if a configuration entry exists for the record
          var detail = trackedRecordEntry.getValue();
          return !this.cloudflareConfiguration.entries().contains(detail.configurationEntry());
        }

        // always delete service records
        return true;
      })
      .map(Map.Entry::getValue)
      .map(recordManager::deleteRecord)
      .toArray(CompletableFuture[]::new);

    // create one final future from all deletion futures and wait
    // for them to complete or time them out after 15 seconds
    CompletableFuture.allOf(deletionFutures)
      .orTimeout(15, TimeUnit.SECONDS)
      .exceptionally(ex -> null)
      .join();
  }

  public @NonNull CloudflareConfiguration configuration() {
    return this.cloudflareConfiguration;
  }

  public void updateConfiguration(@NonNull CloudflareConfiguration cloudflareConfiguration) {
    this.cloudflareConfiguration = cloudflareConfiguration;
    this.writeConfig(Document.newJsonDocument().appendTree(cloudflareConfiguration));
  }

  private void createRecordsForEntries(
    @NonNull @Service I18n i18n,
    @NonNull Configuration nodeConfig,
    @NonNull CloudFlareRecordManager recordManager,
    @NonNull Collection<CloudflareConfigurationEntry> entries
  ) {
    for (var entry : entries) {
      if (entry.enabled()) {
        // build a record which we can create if possible
        var record = this.buildRecord(entry);
        if (record != null) {
          // create a new record for the entry
          recordManager.createRecord(NODE_RECORDS_ID, entry, record).thenAccept(detail -> {
            // check if the record was created
            if (detail != null) {
              LOGGER.info(i18n.translate(
                "module-cloudflare-create-dns-record-for-service",
                entry.domainName(),
                nodeConfig.identity().uniqueId(),
                detail.id()));
            }
          });
        }
      }
    }
  }

  private @Nullable DnsRecord buildRecord(@NonNull CloudflareConfigurationEntry entry) {
    // parse the address info if possible
    var address = this.parseAddressOfEntry(entry);
    if (address == null) {
      return null;
    }

    // create a new record for the entry
    return new DnsRecord(
      address._1(),
      String.format("%s.%s", entry.entryName(), entry.domainName()),
      address._2(),
      1,
      false,
      Document.newJsonDocument());
  }

  private @Nullable Tuple2<DnsType, String> parseAddressOfEntry(@NonNull CloudflareConfigurationEntry entry) {
    try {
      // parse the host address and from that the dns type from the configuration
      var address = InetAddresses.forString(entry.hostAddress());
      var dnsType = address instanceof Inet6Address ? DnsType.AAAA : DnsType.A;

      // return the parsed values
      return new Tuple2<>(dnsType, address.getHostAddress());
    } catch (IllegalArgumentException exception) {
      LOGGER.error("Host address {} is invalid", entry.hostAddress(), exception);
      return null;
    }
  }
}
