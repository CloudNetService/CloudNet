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

import eu.cloudnetservice.common.document.gson.JsonDocument;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.modules.cloudflare.cloudflare.CloudFlareAPI;
import eu.cloudnetservice.modules.cloudflare.dns.DNSType;
import eu.cloudnetservice.modules.cloudflare.dns.DefaultDNSRecord;
import eu.cloudnetservice.modules.cloudflare.listener.CloudflareStartAndStopListener;
import eu.cloudnetservice.node.Node;
import eu.cloudnetservice.node.util.NetworkUtil;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import lombok.NonNull;

public final class CloudNetCloudflareModule extends DriverModule {

  private static final Logger LOGGER = LogManager.logger(CloudNetCloudflareModule.class);

  private CloudFlareAPI cloudFlareAPI;
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
      () -> new CloudflareConfiguration(
        new ArrayList<>(Collections.singletonList(
          new CloudflareConfigurationEntry(
            false,
            CloudflareConfigurationEntry.AuthenticationMethod.GLOBAL_KEY,
            NetworkUtil.localAddress(),
            "user@example.com",
            "api_token_string",
            "zoneId",
            "example.com",
            new ArrayList<>(Collections.singletonList(
              new CloudflareGroupConfiguration("Proxy", "@", 1, 1)
            ))
          )
        ))
      ));
    this.updateConfiguration(configuration);
  }

  @ModuleTask(order = 126, event = ModuleLifeCycle.STARTED)
  public void initCloudflareAPI() {
    this.cloudFlareAPI = new CloudFlareAPI();
  }

  @ModuleTask(order = 125, event = ModuleLifeCycle.STARTED)
  public void addedDefaultCloudflareDNSServices() {
    var cloudConfig = Node.instance().config();

    for (var entry : this.cloudFlareConfiguration().entries()) {
      if (entry.enabled()) {
        boolean ipv6Address;
        try {
          ipv6Address = InetAddress.getByName(entry.hostAddress()) instanceof Inet6Address;
        } catch (UnknownHostException exception) {
          LOGGER.severe("Host address of entry " + entry + " is invalid!", exception);
          continue;
        }

        var recordDetail = this.cloudFlareAPI.createRecord(
          UUID.randomUUID(),
          entry,
          new DefaultDNSRecord(
            ipv6Address ? DNSType.AAAA : DNSType.A,
            cloudConfig.identity().uniqueId() + "." + entry.domainName(),
            entry.hostAddress(),
            JsonDocument.emptyDocument()));
        if (recordDetail != null) {
          LOGGER.info(I18n.trans("module-cloudflare-create-dns-record-for-service", entry.domainName(),
            cloudConfig.identity().uniqueId(), recordDetail.id()));
        }
      }
    }
  }

  @ModuleTask(event = ModuleLifeCycle.RELOADING)
  public void handleReload() {
    this.loadConfiguration();
  }

  @ModuleTask(order = 124, event = ModuleLifeCycle.STARTED)
  public void registerListeners() {
    this.registerListener(new CloudflareStartAndStopListener(this));
  }

  @ModuleTask(order = 64, event = ModuleLifeCycle.STOPPED)
  public void removeRecordsOnDelete() {
    this.cloudFlareAPI.close();
  }

  public @NonNull CloudflareConfiguration cloudFlareConfiguration() {
    return this.cloudflareConfiguration;
  }

  public void updateConfiguration(@NonNull CloudflareConfiguration cloudflareConfiguration) {
    this.cloudflareConfiguration = cloudflareConfiguration;
    this.writeConfig(JsonDocument.newDocument(cloudflareConfiguration));
  }

  public @NonNull CloudFlareAPI cloudFlareAPI() {
    return this.cloudFlareAPI;
  }
}
