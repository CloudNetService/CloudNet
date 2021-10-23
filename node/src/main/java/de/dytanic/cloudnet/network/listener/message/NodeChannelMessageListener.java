/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.network.listener.message;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.cluster.IClusterNodeServerProvider;
import de.dytanic.cloudnet.cluster.sync.DataSyncRegistry;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.IEventManager;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.network.def.NetworkConstants;
import de.dytanic.cloudnet.event.cluster.NetworkClusterNodeInfoUpdateEvent;
import org.jetbrains.annotations.NotNull;

public final class NodeChannelMessageListener {

  private final IEventManager eventManager;
  private final DataSyncRegistry dataSyncRegistry;
  private final IClusterNodeServerProvider nodeServerProvider;

  public NodeChannelMessageListener(
    @NotNull IEventManager eventManager,
    @NotNull DataSyncRegistry dataSyncRegistry,
    @NotNull IClusterNodeServerProvider nodeServerProvider
  ) {
    this.eventManager = eventManager;
    this.dataSyncRegistry = dataSyncRegistry;
    this.nodeServerProvider = nodeServerProvider;
  }

  @EventListener
  public void handleChannelMessage(@NotNull ChannelMessageReceiveEvent event) {
    if (event.getChannel().equals(NetworkConstants.INTERNAL_MSG_CHANNEL) && event.getMessage() != null) {
      switch (event.getMessage()) {
        // update a single node info snapshot
        case "update_node_info_snapshot": {
          NetworkClusterNodeInfoSnapshot snapshot = event.getContent().readObject(NetworkClusterNodeInfoSnapshot.class);
          // get the associated node server
          IClusterNodeServer server = this.nodeServerProvider.getNodeServer(snapshot.getNode().getUniqueId());
          if (server != null) {
            server.setNodeInfoSnapshot(snapshot);
            this.eventManager.callEvent(new NetworkClusterNodeInfoUpdateEvent(event.getNetworkChannel(), snapshot));
          }
        }
        break;
        // handles the sync requests of cluster data
        case "sync_cluster_data": {
          // handle the sync and send back the data to override on the caller
          DataBuf result = this.dataSyncRegistry.handle(event.getContent(), event.getContent().readBoolean());
          if (result != null && event.isQuery()) {
            event.setBinaryResponse(result);
          }
        }
        break;
        // handles the shutdown of a cluster node
        case "cluster_node_shutdown": {
          CloudNet.getInstance().stop();
        }
        break;
        // none of our business
        default:
          break;
      }
    }
  }
}
