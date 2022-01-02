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

package eu.cloudnetservice.modules.cloudflare.listener;

import eu.cloudnetservice.cloudnet.common.language.I18n;
import eu.cloudnetservice.cloudnet.common.log.LogManager;
import eu.cloudnetservice.cloudnet.common.log.Logger;
import eu.cloudnetservice.cloudnet.driver.event.EventListener;
import eu.cloudnetservice.cloudnet.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.cloudnet.node.event.service.CloudServicePostLifecycleEvent;
import eu.cloudnetservice.cloudnet.node.service.CloudService;
import eu.cloudnetservice.modules.cloudflare.CloudNetCloudflareModule;
import eu.cloudnetservice.modules.cloudflare.CloudflareConfigurationEntry;
import eu.cloudnetservice.modules.cloudflare.CloudflareGroupConfiguration;
import eu.cloudnetservice.modules.cloudflare.cloudflare.CloudFlareAPI;
import eu.cloudnetservice.modules.cloudflare.dns.SRVRecord;
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
