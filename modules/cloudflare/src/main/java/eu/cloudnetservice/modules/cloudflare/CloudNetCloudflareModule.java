/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.cloudflare;

import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import eu.cloudnetservice.common.StringUtil;
import eu.cloudnetservice.common.document.gson.JsonDocument;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.modules.cloudflare.cloudflare.CloudFlareRecordManager;
import eu.cloudnetservice.modules.cloudflare.config.CloudflareConfiguration;
import eu.cloudnetservice.modules.cloudflare.config.CloudflareConfigurationEntry;
import eu.cloudnetservice.modules.cloudflare.config.CloudflareGroupConfiguration;
import eu.cloudnetservice.modules.cloudflare.dns.DNSRecord;
import eu.cloudnetservice.modules.cloudflare.dns.DNSType;
import eu.cloudnetservice.modules.cloudflare.listener.CloudflareStartAndStopListener;
import eu.cloudnetservice.node.Node;
import eu.cloudnetservice.node.util.NetworkUtil;
import java.net.Inet6Address;
import java.util.UUID;
import lombok.NonNull;

public final class CloudNetCloudflareModule extends DriverModule {

  private static final UUID NODE_RECORDS_ID = UUID.randomUUID();
  private static final Logger LOGGER = LogManager.logger(CloudNetCloudflareModule.class);

  private final CloudFlareRecordManager recordManager = new CloudFlareRecordManager();
  private CloudflareConfiguration cloudflareConfiguration;

  @ModuleTask(event = ModuleLifeCycle.LOADED)
  public void convertConfiguration() {
    var config = this.readConfig().get("config");
    if (config != null) {
      this.writeConfig(JsonDocument.newDocument(config));
    }
  }

  @ModuleTask(order = 127, event = ModuleLifeCycle.STARTED)
  public void loadConfiguration() {
    var configuration = this.readConfig(
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
        Lists.newArrayList(new CloudflareGroupConfiguration("Proxy", "@", 1, 1))))));
    this.updateConfiguration(configuration);
  }

  @ModuleTask(order = 125, event = ModuleLifeCycle.STARTED)
  public void createRecordsForConfigurationEntries() {
    var nodeConfig = Node.instance().config();

    for (var entry : this.cloudFlareConfiguration().entries()) {
      if (entry.enabled()) {
        try {
          // parse the host address and from that the dns type from the configuration
          var address = InetAddresses.forString(entry.hostAddress());
          var dnsType = address instanceof Inet6Address ? DNSType.AAAA : DNSType.A;

          // create a new record for the entry
          this.recordManager.createRecord(
            NODE_RECORDS_ID,
            entry,
            new DNSRecord(
              dnsType,
              String.format("%s.%s", entry.entryName(), entry.domainName()),
              address.getHostAddress(),
              1,
              false,
              JsonDocument.emptyDocument())
          ).thenAccept(record -> {
            // check if the record was created
            if (record != null) {
              LOGGER.info(I18n.trans(
                "module-cloudflare-create-dns-record-for-service",
                entry.domainName(),
                nodeConfig.identity().uniqueId(),
                record.id()));
            }
          });
        } catch (IllegalArgumentException exception) {
          LOGGER.severe("Host address %s is invalid", exception, entry.hostAddress());
        }
      }
    }
  }

  @ModuleTask(event = ModuleLifeCycle.RELOADING)
  public void handleReload() {
    this.loadConfiguration();
    this.createRecordsForConfigurationEntries();
  }

  @ModuleTask(order = 124, event = ModuleLifeCycle.STARTED)
  public void registerListeners() {
    this.registerListener(new CloudflareStartAndStopListener(this));
  }

  @ModuleTask(order = 64, event = ModuleLifeCycle.STOPPED)
  public void removeRecordsOnDelete() {
    this.recordManager.close();
  }

  public @NonNull CloudflareConfiguration cloudFlareConfiguration() {
    return this.cloudflareConfiguration;
  }

  public void updateConfiguration(@NonNull CloudflareConfiguration cloudflareConfiguration) {
    this.cloudflareConfiguration = cloudflareConfiguration;
    this.writeConfig(JsonDocument.newDocument(cloudflareConfiguration));
  }

  public @NonNull CloudFlareRecordManager recordManager() {
    return this.recordManager;
  }
}
