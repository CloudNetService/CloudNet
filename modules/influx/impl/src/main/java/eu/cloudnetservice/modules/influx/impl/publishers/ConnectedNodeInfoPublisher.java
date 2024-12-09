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
import eu.cloudnetservice.driver.cluster.NodeInfoSnapshot;
import eu.cloudnetservice.driver.registry.AutoService;
import eu.cloudnetservice.modules.influx.impl.util.PointUtil;
import eu.cloudnetservice.modules.influx.publish.Publisher;
import eu.cloudnetservice.node.cluster.NodeServer;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import jakarta.inject.Singleton;
import java.util.Collection;
import java.util.Objects;
import lombok.NonNull;

@Singleton
@AutoService(services = Publisher.class, name = "ConnectedNodeInfo")
public record ConnectedNodeInfoPublisher(@NonNull NodeServerProvider nodeServerProvider) implements Publisher {

  @Override
  public @NonNull Collection<Point> createPoints() {
    return this.nodeServerProvider.nodeServers().stream()
      .map(NodeServer::nodeInfoSnapshot)
      .filter(Objects::nonNull)
      .map(this::createPoint)
      .toList();
  }

  private @NonNull Point createPoint(@NonNull NodeInfoSnapshot snapshot) {
    return PointUtil.point("nodes")
      .addTag("name", snapshot.node().uniqueId())
      .addField("ServiceMaxMemory", snapshot.maxMemory())
      .addField("ServiceUsedMemory", snapshot.usedMemory())
      .addField("ServiceReservedMemory", snapshot.reservedMemory())
      .addField("ServiceCount", snapshot.currentServicesCount())
      .addField("UsedCpu", snapshot.processSnapshot().systemCpuUsage())
      .addField("Threads", snapshot.processSnapshot().threads().size())
      .addField("MaxMemory", snapshot.processSnapshot().maxHeapMemory())
      .addField("UsedMemory", snapshot.processSnapshot().heapUsageMemory())
      .addField("LoadedClassCount", snapshot.processSnapshot().currentLoadedClassCount());
  }
}
