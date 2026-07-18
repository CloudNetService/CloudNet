/*
 * Copyright 2019-2025 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.dns.impl.listener;

import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.modules.dns.config.DnsModuleConfigEntry;
import eu.cloudnetservice.modules.dns.config.DnsModuleGroupEntry;
import eu.cloudnetservice.modules.dns.impl.CloudNetDnsModule;
import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.event.service.CloudServicePostLifecycleEvent;
import eu.cloudnetservice.node.service.CloudService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.function.BiConsumer;
import lombok.NonNull;

@Singleton
public final class DnsServiceListener {

  private final CloudNetDnsModule module;

  @Inject
  public DnsServiceListener(@NonNull CloudNetDnsModule module) {
    this.module = module;
  }

  @EventListener
  private void handleServiceStart(
    @NonNull CloudServicePostLifecycleEvent event,
    @NonNull Configuration configuration,
    @NonNull ServiceRegistry registry
  ) {
    if (event.newLifeCycle() == ServiceLifeCycle.RUNNING) {
      this.syncService(event.service(), configuration, registry);
    } else {
      this.deleteServiceRecords(event.service(), configuration, registry);
    }
  }

  public void syncService(
    @NonNull CloudService service,
    @NonNull Configuration configuration,
    @NonNull ServiceRegistry registry
  ) {
    if (service.lifeCycle() == ServiceLifeCycle.RUNNING) {
      this.handleWithConfiguration(service, (config, group) -> {
        var zoneProvider = this.module.zoneProvider(registry, config);
        if (zoneProvider != null) {
          this.module.syncServiceRecords(zoneProvider, config, group, service, configuration);
        }
      });
    }
  }

  public void deleteServiceRecords(
    @NonNull CloudService service,
    @NonNull Configuration configuration,
    @NonNull ServiceRegistry registry
  ) {
    this.handleWithConfiguration(service, (config, group) -> {
      var zoneProvider = this.module.zoneProvider(registry, config);
      if (zoneProvider != null) {
        this.module.deleteServiceRecords(zoneProvider, config, group, service, configuration);
      }
    });
  }

  private void handleWithConfiguration(
    @NonNull CloudService targetService,
    @NonNull BiConsumer<DnsModuleConfigEntry, DnsModuleGroupEntry> handler
  ) {
    for (var entry : this.module.configuration().entries()) {
      if (entry != null && entry.enabled() && !entry.groups().isEmpty()) {
        for (var config : entry.groups()) {
          if (config != null && targetService.serviceConfiguration().groups().contains(config.targetGroup())) {
            handler.accept(entry, config);
          }
        }
      }
    }
  }
}
