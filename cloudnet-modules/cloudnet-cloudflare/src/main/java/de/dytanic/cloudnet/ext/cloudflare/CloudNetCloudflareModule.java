/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.ext.cloudflare;

import com.google.gson.JsonObject;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.ext.cloudflare.cloudflare.CloudFlareAPI;
import de.dytanic.cloudnet.ext.cloudflare.cloudflare.DnsRecordDetail;
import de.dytanic.cloudnet.ext.cloudflare.dns.DNSType;
import de.dytanic.cloudnet.ext.cloudflare.dns.DefaultDNSRecord;
import de.dytanic.cloudnet.ext.cloudflare.http.V1CloudflareConfigurationHttpHandler;
import de.dytanic.cloudnet.ext.cloudflare.listener.CloudflareStartAndStopListener;
import de.dytanic.cloudnet.module.NodeCloudNetModule;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

public final class CloudNetCloudflareModule extends NodeCloudNetModule {

  private static CloudNetCloudflareModule instance;

  private CloudFlareAPI cloudFlareAPI;
  private CloudflareConfiguration cloudflareConfiguration;

  public CloudNetCloudflareModule() {
    instance = this;
  }

  public static CloudNetCloudflareModule getInstance() {
    return CloudNetCloudflareModule.instance;
  }

  @ModuleTask(order = 127, event = ModuleLifeCycle.STARTED)
  public void loadConfiguration() {
    this.cloudflareConfiguration = this.getConfig()
      .get("config", CloudflareConfiguration.TYPE, new CloudflareConfiguration(
        new ArrayList<>(Collections.singletonList(
          new CloudflareConfigurationEntry(
            false,
            this.getInitialHostAddress(),
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

    this.updateConfiguration(this.cloudflareConfiguration);
  }

  @ModuleTask(order = 126, event = ModuleLifeCycle.STARTED)
  public void initCloudflareAPI() {
    this.cloudFlareAPI = new CloudFlareAPI();
  }

  @ModuleTask(order = 125, event = ModuleLifeCycle.STARTED)
  public void addedDefaultCloudflareDNSServices() {
    for (CloudflareConfigurationEntry entry : this.getCloudflareConfiguration().getEntries()) {
      if (entry.isEnabled()) {
        boolean ipv6Address;
        try {
          ipv6Address = InetAddress.getByName(entry.getHostAddress()) instanceof Inet6Address;
        } catch (UnknownHostException exception) {
          this.getLogger().fatal("Host address of entry " + entry + " is invalid!", exception);
          continue;
        }

        DnsRecordDetail recordDetail = this.cloudFlareAPI.createRecord(
          UUID.randomUUID(),
          entry,
          new DefaultDNSRecord(
            ipv6Address ? DNSType.AAAA : DNSType.A,
            this.getCloudNetConfig().getIdentity().getUniqueId() + "." + entry.getDomainName(),
            entry.getHostAddress(),
            new JsonObject()
          )
        );
        if (recordDetail != null) {
          CloudNetDriver.getInstance().getLogger()
            .info(LanguageManager.getMessage("module-cloudflare-create-dns-record-for-service")
              .replace("%service%", this.getCloudNet().getConfig().getIdentity().getUniqueId())
              .replace("%domain%", entry.getDomainName())
              .replace("%recordId%", recordDetail.getId())
            );
        }
      }
    }
  }

  @ModuleTask(order = 124, event = ModuleLifeCycle.STARTED)
  public void registerListeners() {
    this.registerListener(new CloudflareStartAndStopListener(this.cloudFlareAPI));
  }

  @ModuleTask(order = 123, event = ModuleLifeCycle.STARTED)
  public void registerHttpHandlers() {
    this.getHttpServer().registerHandler(
      "/api/v1/modules/cloudflare/config",
      new V1CloudflareConfigurationHttpHandler("cloudnet.http.v1.modules.cloudflare.config")
    );
  }

  public void updateConfiguration(CloudflareConfiguration cloudflareConfiguration) {
    this.cloudflareConfiguration = cloudflareConfiguration;

    this.getConfig().append("config", cloudflareConfiguration);
    this.saveConfig();
  }

  @ModuleTask(order = 64, event = ModuleLifeCycle.STOPPED)
  public void removeRecordsOnDelete() {
    this.cloudFlareAPI.close();
  }

  private String getInitialHostAddress() {
    try {
      return InetAddress.getLocalHost().getHostAddress();
    } catch (Exception ex) {
      return "0.0.0.0";
    }
  }

  public CloudflareConfiguration getCloudflareConfiguration() {
    return this.cloudflareConfiguration;
  }

  public void setCloudflareConfiguration(CloudflareConfiguration cloudflareConfiguration) {
    this.cloudflareConfiguration = cloudflareConfiguration;
  }

  public CloudFlareAPI getCloudFlareAPI() {
    return this.cloudFlareAPI;
  }
}
