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
import eu.cloudnetservice.modules.dns.provider.DnsProvider;
import eu.cloudnetservice.modules.dns.provider.record.SrvDnsRecordData;
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
      this.handleWithConfiguration(event.service(), (config, group) -> {
        var hostAddressV4 = this.module.resolveHostAddress(group.hostAddressV4(), configuration);
        if (hostAddressV4 == null) {
          hostAddressV4 = this.module.resolveHostAddress(config.hostAddressV4(), configuration);
        }

        if (hostAddressV4 != null) {
          // TODO das ist doch yek
          var zoneProvider = registry.instance(
            DnsProvider.class,
            config.providerConfig().name()
          ).zoneProvider(config.providerConfig().toProviderZoneConfig());

          for (var record : group.records()) {
            zoneProvider.createDnsRecord(new SrvDnsRecordData(
              record.subdomain(),
              record.ttl(),
              "wtf-was-für-ziel-ja",
              event.service().serviceConfiguration().port(),
              record.priority(),
              record.weight()));
          }
        }
      });
    }
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
