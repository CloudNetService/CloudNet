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
import de.dytanic.cloudnet.service.CloudService;
import java.util.function.BiConsumer;
import lombok.NonNull;

public final class CloudflareStartAndStopListener {

  private static final Logger LOGGER = LogManager.logger(CloudflareStartAndStopListener.class);

  private final CloudFlareAPI cloudFlareAPI;

  public CloudflareStartAndStopListener(CloudFlareAPI cloudFlareAPI) {
    this.cloudFlareAPI = cloudFlareAPI;
  }

  @EventListener
  public void handlePostStart(@NonNull CloudServicePostLifecycleEvent event) {
    if (event.newLifeCycle() == ServiceLifeCycle.RUNNING) {
      this.handleWithConfiguration(event.service(), (entry, configuration) -> {
        // create the new record
        var recordDetail = this.cloudFlareAPI.createRecord(
          event.service().serviceId().uniqueId(),
          entry,
          SRVRecord.forConfiguration(entry, configuration, event.service().serviceConfiguration().port()));
        // publish a message to the node log if the record was created successfully
        if (recordDetail != null) {
          LOGGER.info(I18n.trans("module-cloudflare-create-dns-record-for-service")
            .replace("%service%", event.service().serviceId().name())
            .replace("%domain%", entry.domainName())
            .replace("%recordId%", recordDetail.id()));
        }
      });
    }
  }

  @EventListener
  public void handlePostStop(@NonNull CloudServicePostLifecycleEvent event) {
    if (event.newLifeCycle() == ServiceLifeCycle.STOPPED || event.newLifeCycle() == ServiceLifeCycle.DELETED) {
      this.handleWithConfiguration(event.service(), (entry, configuration) -> {
        // delete all records of the the service
        for (var detail : this.cloudFlareAPI.deleteAllRecords(event.service())) {
          LOGGER.info(I18n.trans("module-cloudflare-delete-dns-record-for-service")
            .replace("%service%", event.service().serviceId().name())
            .replace("%domain%", entry.domainName())
            .replace("%recordId%", detail.id()));
        }
      });
    }
  }

  private void handleWithConfiguration(
    @NonNull CloudService targetService,
    @NonNull BiConsumer<CloudflareConfigurationEntry, CloudflareGroupConfiguration> handler
  ) {
    for (var entry : CloudNetCloudflareModule.instance().cloudFlareConfiguration().entries()) {
      if (entry != null && entry.enabled() && !entry.groups().isEmpty()) {
        for (var config : entry.groups()) {
          if (config != null && targetService.serviceConfiguration().groups().contains(config.name())) {
            handler.accept(entry, config);
          }
        }
      }
    }
  }
}
