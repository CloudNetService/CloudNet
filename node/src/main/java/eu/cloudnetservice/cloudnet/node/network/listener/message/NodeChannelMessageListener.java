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

package eu.cloudnetservice.cloudnet.node.network.listener.message;

import eu.cloudnetservice.cloudnet.driver.event.EventListener;
import eu.cloudnetservice.cloudnet.driver.event.EventManager;
import eu.cloudnetservice.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import eu.cloudnetservice.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import eu.cloudnetservice.cloudnet.driver.network.def.NetworkConstants;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.cloudnet.node.cluster.ClusterNodeServerProvider;
import eu.cloudnetservice.cloudnet.node.cluster.sync.DataSyncRegistry;
import eu.cloudnetservice.cloudnet.node.event.cluster.NetworkClusterNodeInfoUpdateEvent;
import lombok.NonNull;

public final class NodeChannelMessageListener {

  private final EventManager eventManager;
  private final DataSyncRegistry dataSyncRegistry;
  private final ClusterNodeServerProvider nodeServerProvider;

  public NodeChannelMessageListener(
    @NonNull EventManager eventManager,
    @NonNull DataSyncRegistry dataSyncRegistry,
    @NonNull ClusterNodeServerProvider nodeServerProvider
  ) {
    this.eventManager = eventManager;
    this.dataSyncRegistry = dataSyncRegistry;
    this.nodeServerProvider = nodeServerProvider;
  }

  @EventListener
  public void handleChannelMessage(@NonNull ChannelMessageReceiveEvent event) {
    if (event.channel().equals(NetworkConstants.INTERNAL_MSG_CHANNEL)) {
      switch (event.message()) {
        // update a single node info snapshot
        case "update_node_info_snapshot" -> {
          var snapshot = event.content().readObject(NetworkClusterNodeInfoSnapshot.class);
          // get the associated node server
          var server = this.nodeServerProvider.nodeServer(snapshot.node().uniqueId());
          if (server != null) {
            server.nodeInfoSnapshot(snapshot);
            this.eventManager.callEvent(new NetworkClusterNodeInfoUpdateEvent(event.networkChannel(), snapshot));
          }
        }

        // handles the sync requests of cluster data
        case "sync_cluster_data" -> {
          // handle the sync and send back the data to override on the caller
          var result = this.dataSyncRegistry.handle(event.content(), event.content().readBoolean());
          if (result != null && event.query()) {
            event.binaryResponse(result);
          }
        }

        // handles the shutdown of a cluster node
        case "cluster_node_shutdown" -> CloudNet.instance().stop();

        // request of the full cluster data set
        case "request_initial_cluster_data" -> {
          var server = this.nodeServerProvider.nodeServer(event.networkChannel());
          if (server != null) {
            // do not force the sync - the user can decide which changes should be used
            server.syncClusterData(CloudNet.instance().config().forceInitialClusterDataSync());
          }
        }

        // none of our business
        default -> {
        }
      }
    }
  }
}
