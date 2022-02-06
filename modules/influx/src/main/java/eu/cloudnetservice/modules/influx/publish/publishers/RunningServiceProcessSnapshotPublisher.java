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

package eu.cloudnetservice.modules.influx.publish.publishers;

import com.influxdb.client.write.Point;
import eu.cloudnetservice.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.modules.bridge.BridgeServiceProperties;
import eu.cloudnetservice.modules.influx.publish.Publisher;
import eu.cloudnetservice.modules.influx.util.PointUtil;
import java.util.Collection;
import lombok.NonNull;

public final class RunningServiceProcessSnapshotPublisher implements Publisher {

  @Override
  public @NonNull Collection<Point> createPoints() {
    return CloudNetDriver.instance().cloudServiceProvider().runningServices().stream()
      .map(service -> PointUtil.point("services")
        .addTag("Name", service.name())
        .addTag("Task", service.serviceId().taskName())
        .addTag("Environment", service.serviceId().environmentName())
        .addField("UsedCpu", service.processSnapshot().cpuUsage())
        .addField("Threads", service.processSnapshot().threads().size())
        .addField("MaxMemory", service.processSnapshot().maxHeapMemory())
        .addField("UsedMemory", service.processSnapshot().heapUsageMemory())
        .addField("LoadedClassCount", service.processSnapshot().currentLoadedClassCount())
        .addField("MaxPlayers", BridgeServiceProperties.MAX_PLAYERS.read(service).orElse(0))
        .addField("OnlinePlayers", BridgeServiceProperties.ONLINE_COUNT.read(service).orElse(0))
        .addField("Full", BridgeServiceProperties.IS_FULL.read(service).orElse(false))
        .addField("Empty", BridgeServiceProperties.IS_EMPTY.read(service).orElse(false))
        .addField("Online", BridgeServiceProperties.IS_ONLINE.read(service).orElse(false))
        .addField("Ingame", BridgeServiceProperties.IS_IN_GAME.read(service).orElse(false))
        .addField("Starting", BridgeServiceProperties.IS_STARTING.read(service).orElse(false))
      ).toList();
  }
}
