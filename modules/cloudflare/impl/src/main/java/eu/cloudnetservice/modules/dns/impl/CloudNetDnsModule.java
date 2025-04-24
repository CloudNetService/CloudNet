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

package eu.cloudnetservice.modules.dns.impl;

import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.modules.dns.config.DnsModuleConfig;
import eu.cloudnetservice.modules.dns.config.DnsModuleConfigEntry;
import eu.cloudnetservice.modules.dns.config.DnsModuleGroupEntry;
import eu.cloudnetservice.modules.dns.config.DnsModuleGroupRecord;
import eu.cloudnetservice.modules.dns.config.DnsModuleProviderConfig;
import eu.cloudnetservice.modules.dns.impl._depreacted.CloudflareConfiguration;
import eu.cloudnetservice.modules.dns.impl._depreacted.CloudflareConfigurationEntry;
import eu.cloudnetservice.modules.dns.provider.DnsProvider;
import eu.cloudnetservice.modules.dns.provider.DnsZoneProvider;
import eu.cloudnetservice.modules.dns.provider.info.DnsRecordInfo;
import eu.cloudnetservice.modules.dns.provider.record.AAAADnsRecordData;
import eu.cloudnetservice.modules.dns.provider.record.ADnsRecordData;
import eu.cloudnetservice.modules.dns.provider.record.DnsRecordData;
import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.impl.util.NetworkUtil;
import jakarta.inject.Singleton;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class CloudNetDnsModule extends DriverModule {

  private static final String LEGACY_MODULE_NAME = "CloudNet-Cloudflare";
  private static final Logger LOGGER = LoggerFactory.getLogger(CloudNetDnsModule.class);

  private DnsModuleConfig configuration;

  @ModuleTask(lifecycle = ModuleLifeCycle.LOADED)
  public void convertConfiguration() {
    var documentFactory = DocumentFactory.json();
    if (this.readConfig(documentFactory).empty()) {
      // TODO was wenn der user gar keine alte config hat und das der erste start ist
      var legacyConfigPath = this.configPath()
        .resolve("../")
        .resolve(LEGACY_MODULE_NAME)
        .resolve("config.json");

      if (Files.notExists(legacyConfigPath)) {
        return;
      }

      var legacyConfig = documentFactory.parse(legacyConfigPath).toInstanceOf(CloudflareConfiguration.class);
      var targetConfig = new DnsModuleConfig(new ArrayList<>());
      for (var entry : legacyConfig.entries()) {
        if (entry.authenticationMethod() == CloudflareConfigurationEntry.AuthenticationMethod.GLOBAL_KEY) {
          LOGGER.warn(
            "Unable to convert configuration for entry {}: global key authentication is not supported",
            entry.entryName());
          continue;
        }

        var groupEntries = entry.groups().stream()
          .map(group -> DnsModuleGroupEntry.builder()
            .targetGroup(group.name())
            .records(List.of(DnsModuleGroupRecord.builder()
              .priority(group.priority())
              .weight(group.weight())
              .subdomain(group.sub())
              .build()))
            .build())
          .toList();

        var targetEntry = DnsModuleConfigEntry.builder()
          .groups(groupEntries)
          .enabled(entry.enabled())
          .domain(entry.domainName())
          .hostAddressV4(entry.hostAddress())
          .providerConfig(DnsModuleProviderConfig.builder()
            .name("cloudflare")
            .properties(documentFactory.newDocument()
              .append("apiKey", entry.apiToken())
              .append("zoneId", entry.zoneId()))
            .build())
          .build();

        targetConfig.entries().add(targetEntry);
      }

      this.writeConfig(documentFactory.newDocument(targetConfig));
    }
  }

  @ModuleTask(order = Byte.MAX_VALUE, lifecycle = ModuleLifeCycle.STARTED)
  public void discoverServices(@NonNull ServiceRegistry registry) {
    registry.discoverServices(CloudNetDnsModule.class);
  }

  @ModuleTask(order = 120, lifecycle = ModuleLifeCycle.STARTED)
  public void loadConfiguration() {
    this.configuration = this.readConfig(
      DnsModuleConfig.class,
      // TODO: proper default config
      () -> new DnsModuleConfig(List.of(
        DnsModuleConfigEntry.builder()
          .enabled(true)
          .hostAddressV4("127.0.0.1")
          .domain("cloudnetservice.eu")
          .groups(List.of(DnsModuleGroupEntry.builder()
            .targetGroup("Proxy")
            .build()))
          .providerConfig(DnsModuleProviderConfig.builder()
            .name("cloudflare")
            .properties(DocumentFactory.json().newDocument()
              .append("apiKey", "")
              .append("zoneId", ""))
            .build())
          .build()
      )),
      DocumentFactory.json());
  }

  /*

  "domain": "playo.dev",
      "domainNamespace": null,
      "hostAddressV4": "127.0.0.1",
      "hostAddressV6": "0:0:0:0:0:0:0:1",
      "groups": [
        {
          "targetGroup": "Proxy",
          "hostAddressV4": null,
          "hostAddressV6": null,
          "records": [
            {
              "subdomain": "@"
             },
             {
              "subdomain": "velocity",
              }
          ]
        }
      ],

      -> A node-1.playo.dev -> 127.0.0.1
      -> AAAA node-1.playo.dev -> 0:0:0:0:0:0:0:1

      Für jeden Proxy:

      -> SRV playo.dev -> PORT node-1.playo.dev
      -> SRV velocity.playo.dev -> PORT node-1.playo.dev



      "domain": "playo.dev",
      "domainNamespace": null,
      "hostAddressV4": "127.0.0.1",
      "hostAddressV6": "0:0:0:0:0:0:0:1",
      "groups": [
        {
          "targetGroup": "Proxy",
          "hostAddressV4": "10.10.10.10",
          "hostAddressV6": null,
          "records": [
            {
              "subdomain": "@"
             },
             {
              "subdomain": "velocity",
              }
          ]
        }
      ],

      -> A node-1.playo.dev -> 127.0.0.1
      -> AAAA node-1.playo.dev -> 0:0:0:0:0:0:0:1
      -> A md5(node-1-Proxy-10.10.10.10).playo.dev -> 10.10.10.10
      -> KEIN AAAA Record da null

      Für jeden Proxy:

      -> SRV playo.dev -> PORT md5(node-1-Proxy-10.10.10.10).playo.dev
      -> SRV velocity.playo.dev -> PORT md5(node-1-Proxy-10.10.10.10).playo.dev

      "domain": "playo.dev",
      "domainNamespace": null,
      "hostAddressV4": "127.0.0.1",
      "hostAddressV6": "0:0:0:0:0:0:0:1",
      "groups": [
        {
          "targetGroup": "Proxy",
          "hostAddressV4": "10.10.10.10",
          "hostAddressV6": "fd00::1",
          "records": [
            {
              "subdomain": "@"
             },
             {
              "subdomain": "velocity",
              }
          ]
        }
      ],

      -> A node-1.playo.dev -> 127.0.0.1
      -> AAAA node-1.playo.dev -> 0:0:0:0:0:0:0:1
      -> A md5(node-1-Proxy-10.10.10.10).playo.dev -> 10.10.10.10
      -> AAAA md5(node-1-Proxy-fd00::1).playo.dev -> fd00::1

      Für jeden Proxy:
      -> SRV playo.dev -> PORT md5(node-1-Proxy-10.10.10.10).playo.dev
      -> SRV playo.dev -> PORT md5(node-1-Proxy-fd00::1).playo.dev
      -> SRV velocity.playo.dev -> PORT md5(node-1-Proxy-10.10.10.10).playo.dev
      -> SRV velocity.playo.dev -> PORT md5(node-1-Proxy-fd00::1).playo.dev

   */

  @ModuleTask(order = 110, lifecycle = ModuleLifeCycle.STARTED)
  public void registerDnsProvider(@NonNull ServiceRegistry registry, @NonNull Configuration configuration) {
    for (var entry : this.configuration.entries()) {
      if (entry.enabled()) {
        var providerConfig = entry.providerConfig();
        var provider = registry.instance(DnsProvider.class, providerConfig.name());
        var zoneProvider = provider.zoneProvider(providerConfig.toProviderZoneConfig());

        var resolvedV4Host = this.resolveHostAddress(entry.hostAddressV4(), configuration);
        var resolvedV6Host = this.resolveHostAddress(entry.hostAddressV6(), configuration);
        if (resolvedV4Host == null && resolvedV6Host == null) {
          LOGGER.warn("Unable to resolve host address for entry {}: both host addresses are not valid", entry.domain());
          continue;
        }

        var nodeId = configuration.identity().uniqueId();
        var expectedName = "%s.%s".formatted(nodeId, entry.domain());
        zoneProvider.listRecords().andThen(records -> {
          if (resolvedV4Host != null) {
            this.processNodeRecords(
              zoneProvider,
              records,
              expectedName,
              "A",
              resolvedV4Host,
              name -> new ADnsRecordData(name, 0, resolvedV4Host));
          }

          if (resolvedV6Host != null) {
            this.processNodeRecords(
              zoneProvider,
              records,
              expectedName,
              "AAAA",
              resolvedV6Host,
              name -> new AAAADnsRecordData(name, 0, resolvedV6Host));
          }
        }).andThen(records -> {
          // store records
          })
          .onFailure(throwable -> LOGGER.warn("Could read initial records for zone {}: {}", entry.domain(),
          throwable.getMessage()));
      }
    }
  }

  public @NonNull DnsModuleConfig configuration() {
    return this.configuration;
  }

  // TODO consider if this should be public and maybe also might go into a different class at all
  public @Nullable String resolveHostAddress(@Nullable String hostAddress, @NonNull Configuration config) {
    if (hostAddress == null || hostAddress.isEmpty()) {
      return null;
    }

    var parsedHost = NetworkUtil.parseHostAndPort(hostAddress, false);
    if (parsedHost != null) {
      return parsedHost.host();
    }

    var alias = config.ipAliases().get(hostAddress);
    if (alias != null) {
      var parsedAlias = NetworkUtil.parseHostAndPort(alias, false);
      if (parsedAlias != null) {
        return parsedAlias.host();
      }
    }

    return null;
  }

  private void processNodeRecords(
    @NonNull DnsZoneProvider zoneProvider,
    @NonNull List<DnsRecordInfo> records,
    @NonNull String expectedName,
    @NonNull String type,
    @NonNull String resolvedHost,
    @NonNull Function<String, DnsRecordData> recordFactory
  ) {
    var existingRecord = records.stream()
      .filter(record -> record.data().name().equalsIgnoreCase(expectedName))
      .filter(record -> record.data().type().equalsIgnoreCase(type))
      .findFirst()
      .orElse(null);

    if (existingRecord == null) {
      LOGGER.debug("Creating new DNS record for {} with type {} and content {}", expectedName, type, resolvedHost);
      zoneProvider.createDnsRecord(recordFactory.apply(expectedName));
    } else {
      var data = existingRecord.data();
      if (!data.content().equals(resolvedHost)) {
        LOGGER.debug("Updating DNS record for {} with type {} and content {}", expectedName, type, resolvedHost);
        zoneProvider.updateDnsRecord(existingRecord, recordFactory.apply(expectedName));
      } else {
        LOGGER.debug("DNS record for {} with type {} and content {} already exists", expectedName, type, resolvedHost);
      }
    }
  }
}
