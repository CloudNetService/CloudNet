/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.influx.publish.publishers;

import com.influxdb.client.write.Point;
import eu.cloudnetservice.modules.bridge.BridgeServiceProperties;
import eu.cloudnetservice.modules.influx.publish.Publisher;
import eu.cloudnetservice.modules.influx.util.PointUtil;
import eu.cloudnetservice.node.service.CloudServiceManager;
import jakarta.inject.Singleton;
import java.util.Collection;
import lombok.NonNull;

@Singleton
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
        .addField("MaxPlayers", BridgeServiceProperties.MAX_PLAYERS.readOr(service, 0))
        .addField("OnlinePlayers", BridgeServiceProperties.ONLINE_COUNT.readOr(service, 0))
        .addField("Full", BridgeServiceProperties.IS_FULL.readOr(service, false))
        .addField("Empty", BridgeServiceProperties.IS_EMPTY.readOr(service, false))
        .addField("Online", BridgeServiceProperties.IS_ONLINE.readOr(service, false))
        .addField("Ingame", BridgeServiceProperties.IS_IN_GAME.readOr(service, false))
        .addField("Starting", BridgeServiceProperties.IS_STARTING.readOr(service, false))
      ).toList();
  }
}
