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

package de.dytanic.cloudnet.ext.cloudflare.listener;

import de.dytanic.cloudnet.common.language.I18n;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.event.service.CloudServicePostLifecycleEvent;
import de.dytanic.cloudnet.ext.cloudflare.CloudNetCloudflareModule;
import de.dytanic.cloudnet.ext.cloudflare.CloudflareConfigurationEntry;
import de.dytanic.cloudnet.ext.cloudflare.CloudflareGroupConfiguration;
import de.dytanic.cloudnet.ext.cloudflare.cloudflare.CloudFlareAPI;
import de.dytanic.cloudnet.ext.cloudflare.dns.SRVRecord;
import de.dytanic.cloudnet.service.ICloudService;
import java.util.function.BiConsumer;

public final class CloudflareStartAndStopListener {

  private static final Logger LOGGER = LogManager.logger(CloudflareStartAndStopListener.class);

  private final CloudFlareAPI cloudFlareAPI;

  public CloudflareStartAndStopListener(CloudFlareAPI cloudFlareAPI) {
    this.cloudFlareAPI = cloudFlareAPI;
  }

  @EventListener
  public void handlePostStart(CloudServicePostLifecycleEvent event) {
    if (event.getNewLifeCycle() != ServiceLifeCycle.RUNNING) {
      return;
    }

    this.handle0(event.getService(), (entry, configuration) -> {
      var recordDetail = this.cloudFlareAPI.createRecord(
        event.getService().getServiceId().uniqueId(),
        entry,
        SRVRecord.forConfiguration(entry, configuration, event.getService().getServiceConfiguration().port())
      );

      if (recordDetail != null) {
        LOGGER
          .info(I18n.trans("module-cloudflare-create-dns-record-for-service")
            .replace("%service%", event.getService().getServiceId().name())
            .replace("%domain%", entry.domainName())
            .replace("%recordId%", recordDetail.id())
          );
      }
    });
  }

  @EventListener
  public void handlePostStop(CloudServicePostLifecycleEvent event) {
    if (event.getNewLifeCycle() != ServiceLifeCycle.STOPPED) {
      this.handle0(event.getService(), (entry, configuration) -> {
        for (var detail : this.cloudFlareAPI.deleteAllRecords(event.getService())) {
          LOGGER
            .info(I18n.trans("module-cloudflare-delete-dns-record-for-service")
              .replace("%service%", event.getService().getServiceId().name())
              .replace("%domain%", entry.domainName())
              .replace("%recordId%", detail.id())
            );
        }
      });
    }
  }

  private void handle0(ICloudService cloudService,
    BiConsumer<CloudflareConfigurationEntry, CloudflareGroupConfiguration> handler) {
    for (var entry : CloudNetCloudflareModule.instance().cloudFlareConfiguration()
      .entries()) {
      if (entry != null && entry.enabled() && entry.groups() != null && !entry.groups().isEmpty()) {
        for (var groupConfiguration : entry.groups()) {
          if (groupConfiguration != null
            && cloudService.getServiceConfiguration().groups().contains(groupConfiguration.name())) {
            handler.accept(entry, groupConfiguration);
          }
        }
      }
    }
  }
}
