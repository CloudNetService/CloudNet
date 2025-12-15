/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.modules.bridge.impl.node.listener;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.modules.bridge.BridgeDocProperties;
import eu.cloudnetservice.node.event.service.CloudServicePostLifecycleEvent;
import eu.cloudnetservice.node.service.CloudServiceManager;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;

@Singleton
public class BridgeServiceMetricListener {

  private final MeterRegistry meterRegistry;
  private final Multimap<UUID, Meter.Id> metrics;
  private final CloudServiceManager cloudServiceManager;

  @Inject
  public BridgeServiceMetricListener(
    @NonNull MeterRegistry meterRegistry,
    @NonNull CloudServiceManager cloudServiceManager
  ) {
    this.meterRegistry = meterRegistry;
    this.cloudServiceManager = cloudServiceManager;

    this.metrics = Multimaps.newMultimap(new ConcurrentHashMap<>(), ConcurrentHashMap::newKeySet);
  }

  @EventListener
  private void handleLocalServiceLifecycleChange(@NonNull CloudServicePostLifecycleEvent event) {
    var serviceId = event.serviceInfo().serviceId().uniqueId();
    switch (event.newLifeCycle()) {
      case RUNNING -> {
        var onlinePlayersMetric = Gauge.builder(
            "bridge_service_online_players",
            event.service(),
            (service) -> service.serviceInfo().readPropertyOrDefault(BridgeDocProperties.ONLINE_COUNT, -1))
          .strongReference(false)
          .register(this.meterRegistry)
          .getId();
        this.metrics.put(serviceId, onlinePlayersMetric);
        var maxPlayersMetric = Gauge.builder(
            "bridge_service_max_players",
            event.service(),
            (service) -> service.serviceInfo().readPropertyOrDefault(BridgeDocProperties.MAX_PLAYERS, -1))
          .strongReference(false)
          .register(this.meterRegistry)
          .getId();
        this.metrics.put(serviceId, maxPlayersMetric);
      }
      case STOPPED, DELETED -> {
        for (var id : this.metrics.removeAll(serviceId)) {
          for (Meter meter : this.meterRegistry.getMeters()) {
            meter.getId().get
          }
          this.meterRegistry.remove(id);
        }
      }
    }
  }
}
