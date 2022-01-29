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
import eu.cloudnetservice.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.cloudnet.node.cluster.NodeServer;
import eu.cloudnetservice.cloudnet.node.cluster.NodeServerProvider;
import eu.cloudnetservice.modules.influx.publish.Publisher;
import eu.cloudnetservice.modules.influx.util.PointUtil;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.NonNull;

public final class ConnectedNodeInfoPublisher implements Publisher {

  private final NodeServerProvider<? extends NodeServer> nodeServerProvider;

  public ConnectedNodeInfoPublisher() {
    this.nodeServerProvider = CloudNet.instance().nodeServerProvider();
  }

  @Override
  public @NonNull Collection<Point> createPoints() {
    return this.nodeServerProvider.nodeServers().stream()
      .map(NodeServer::nodeInfoSnapshot)
      .filter(Objects::nonNull)
      .map(this::createPoint)
      .collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
        // add a point for the local node which is not part of the list
        list.add(this.createPoint(this.nodeServerProvider.selfNode().nodeInfoSnapshot()));
        return list;
      }));
  }

  private @NonNull Point createPoint(@NonNull NetworkClusterNodeInfoSnapshot snapshot) {
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
