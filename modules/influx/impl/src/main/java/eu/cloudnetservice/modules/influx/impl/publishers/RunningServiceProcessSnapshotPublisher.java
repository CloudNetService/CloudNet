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

package eu.cloudnetservice.modules.influx.impl.publishers;

import com.influxdb.client.write.Point;
import eu.cloudnetservice.driver.registry.AutoService;
import eu.cloudnetservice.modules.bridge.BridgeDocProperties;
import eu.cloudnetservice.modules.bridge.BridgeServiceHelper;
import eu.cloudnetservice.modules.influx.impl.util.PointUtil;
import eu.cloudnetservice.modules.influx.publish.Publisher;
import eu.cloudnetservice.node.service.CloudServiceManager;
import jakarta.inject.Singleton;
import java.util.Collection;
import lombok.NonNull;

@Singleton
@AutoService(services = Publisher.class, name = "RunningServiceProcessSnapshot")
public record RunningServiceProcessSnapshotPublisher(@NonNull CloudServiceManager serviceManager) implements Publisher {

  @Override
  public @NonNull Collection<Point> createPoints() {
    return this.serviceManager.runningServices().stream()
      .map(service -> PointUtil.point("services")
        .addTag("Name", service.name())
        .addTag("Task", service.serviceId().taskName())
        .addTag("Environment", service.serviceId().environmentName())
        .addField("UsedCpu", service.processSnapshot().cpuUsage())
        .addField("Threads", service.processSnapshot().threads().size())
        .addField("MaxMemory", service.processSnapshot().maxHeapMemory())
        .addField("UsedMemory", service.processSnapshot().heapUsageMemory())
        .addField("LoadedClassCount", service.processSnapshot().currentLoadedClassCount())
        .addField("MaxPlayers", service.readProperty(BridgeDocProperties.MAX_PLAYERS))
        .addField("OnlinePlayers", service.readProperty(BridgeDocProperties.ONLINE_COUNT))
        .addField("Full", BridgeServiceHelper.fullService(service))
        .addField("Empty", BridgeServiceHelper.emptyService(service))
        .addField("Online", service.readProperty(BridgeDocProperties.IS_ONLINE))
        .addField("Ingame", BridgeServiceHelper.inGameService(service))
        .addField("Starting", BridgeServiceHelper.startingService(service))
      ).toList();
  }
}
