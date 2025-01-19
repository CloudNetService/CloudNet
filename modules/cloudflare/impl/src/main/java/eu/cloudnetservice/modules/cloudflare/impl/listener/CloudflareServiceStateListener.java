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

package eu.cloudnetservice.modules.cloudflare.impl.listener;

import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.modules.cloudflare.config.CloudflareConfigurationEntry;
import eu.cloudnetservice.modules.cloudflare.config.CloudflareGroupConfiguration;
import eu.cloudnetservice.modules.cloudflare.impl.CloudNetCloudflareModule;
import eu.cloudnetservice.modules.cloudflare.impl.cloudflare.CloudFlareRecordManager;
import eu.cloudnetservice.modules.cloudflare.impl.dns.SrvRecord;
import eu.cloudnetservice.node.event.service.CloudServicePostLifecycleEvent;
import eu.cloudnetservice.node.service.CloudService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.function.BiConsumer;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class CloudflareServiceStateListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(CloudflareServiceStateListener.class);

  private final CloudNetCloudflareModule module;
  private final CloudFlareRecordManager recordManager;

  @Inject
  public CloudflareServiceStateListener(
    @NonNull CloudNetCloudflareModule module,
    @NonNull CloudFlareRecordManager recordManager
  ) {
    this.module = module;
    this.recordManager = recordManager;
  }

  @EventListener
  public void handlePostStart(@NonNull CloudServicePostLifecycleEvent event, @NonNull @Service I18n i18n) {
    if (event.newLifeCycle() == ServiceLifeCycle.RUNNING) {
      this.handleWithConfiguration(event.service(), (entry, configuration) -> {
        // create the new record
        var record = SrvRecord.forConfiguration(entry, configuration, event.service().serviceConfiguration().port());
        this.recordManager
          .createRecord(event.service().serviceId().uniqueId(), entry, record)
          .thenAccept(detail -> {
            // print out the info about the record if it was created successfully
            if (detail != null) {
              LOGGER.info(i18n.translate(
                "module-cloudflare-create-dns-record-for-service",
                entry.domainName(),
                event.service().serviceId().name(),
                detail.id()));
            }
          });
      });
    }
  }

  @EventListener
  public void handlePostStop(@NonNull CloudServicePostLifecycleEvent event, @NonNull @Service I18n i18n) {
    if (event.newLifeCycle() == ServiceLifeCycle.STOPPED || event.newLifeCycle() == ServiceLifeCycle.DELETED) {
      this.handleWithConfiguration(event.service(), (entry, configuration) -> {
        // delete all records of the service
        for (var record : this.recordManager.getAndRemoveRecords(event.service().serviceId().uniqueId())) {
          this.recordManager.deleteRecord(record).thenAccept(deleted -> {
            // print a message if the record was deleted successfully
            if (deleted) {
              LOGGER.info(i18n.translate(
                "module-cloudflare-delete-dns-record-for-service",
                entry.domainName(),
                event.service().serviceId().name(),
                record.id()));
            }
          });
        }
      });
    }
  }

  private void handleWithConfiguration(
    @NonNull CloudService targetService,
    @NonNull BiConsumer<CloudflareConfigurationEntry, CloudflareGroupConfiguration> handler
  ) {
    for (var entry : this.module.configuration().entries()) {
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
