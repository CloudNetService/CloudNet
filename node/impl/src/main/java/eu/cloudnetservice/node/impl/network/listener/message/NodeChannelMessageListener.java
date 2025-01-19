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

package eu.cloudnetservice.node.impl.network.listener.message;

import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.cluster.NetworkClusterNode;
import eu.cloudnetservice.driver.cluster.NodeInfoSnapshot;
import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.event.events.channel.ChannelMessageReceiveEvent;
import eu.cloudnetservice.driver.impl.network.NetworkConstants;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.cluster.sync.DataSyncRegistry;
import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.event.cluster.NetworkClusterNodeInfoUpdateEvent;
import eu.cloudnetservice.node.impl.provider.NodeClusterNodeProvider;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class NodeChannelMessageListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(NodeChannelMessageListener.class);

  private final EventManager eventManager;
  private final Configuration configuration;
  private final DataSyncRegistry dataSyncRegistry;
  private final NodeClusterNodeProvider nodeInfoProvider;
  private final NodeServerProvider nodeServerProvider;

  @Inject
  public NodeChannelMessageListener(
    @NonNull EventManager eventManager,
    @NonNull Configuration configuration,
    @NonNull DataSyncRegistry dataSyncRegistry,
    @NonNull NodeClusterNodeProvider nodeInfoProvider,
    @NonNull NodeServerProvider nodeServerProvider
  ) {
    this.eventManager = eventManager;
    this.configuration = configuration;
    this.dataSyncRegistry = dataSyncRegistry;
    this.nodeInfoProvider = nodeInfoProvider;
    this.nodeServerProvider = nodeServerProvider;
  }

  @EventListener
  public void handleChannelMessage(@NonNull ChannelMessageReceiveEvent event, @NonNull @Service I18n i18n) {
    if (event.channel().equals(NetworkConstants.INTERNAL_MSG_CHANNEL)) {
      switch (event.message()) {
        // update a single node info snapshot
        case "update_node_info_snapshot" -> {
          var snapshot = event.content().readObject(NodeInfoSnapshot.class);
          // get the associated node server
          var server = this.nodeServerProvider.node(snapshot.node().uniqueId());
          if (server != null) {
            server.updateNodeInfoSnapshot(snapshot);
            this.eventManager.callEvent(new NetworkClusterNodeInfoUpdateEvent(event.networkChannel(), snapshot));
          }
        }

        // handles the sync requests of cluster data
        case "sync_cluster_data" -> {
          // handle the sync and send back the data to override on the caller
          var result = this.dataSyncRegistry.handle(event.content(), event.content().readBoolean());
          if (result != null) {
            // send the response content
            ChannelMessage.builder()
              .message("sync_cluster_data_response")
              .target(event.sender().toTarget())
              .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
              .buffer(result)
              .build()
              .send();
          }
        }

        // handles the response to a cluster data sync
        case "sync_cluster_data_response" -> this.dataSyncRegistry.handle(event.content(), true);

        // handle adding a new cluster node on other nodes
        case "register_known_node" -> {
          // register the node
          var node = event.content().readObject(NetworkClusterNode.class);
          this.nodeInfoProvider.addNodeSilently(node);

          // inform the user
          LOGGER.info(i18n.translate("command-cluster-add-node-success", node.uniqueId()));
        }

        // handle the removal of a cluster node from other nodes
        case "remove_known_node" -> {
          // unregister the node
          var node = event.content().readObject(NetworkClusterNode.class);
          this.nodeInfoProvider.removeNodeSilently(node);

          // remove the node
          LOGGER.info(i18n.translate("command-cluster-remove-node-success", node.uniqueId()));
        }

        // handles the shutdown of a cluster node
        case "cluster_node_shutdown" -> System.exit(0);

        // request of the full cluster data set
        case "request_initial_cluster_data" -> {
          var server = this.nodeServerProvider.node(event.networkChannel());
          if (server != null) {
            // do not force the sync - the user can decide which changes should be used
            server.syncClusterData(this.configuration.forceInitialClusterDataSync());
          }
        }

        // execute a command
        case "send_command_line" -> {
          var response = this.nodeServerProvider.localNode().sendCommandLine(event.content().readString());
          event.binaryResponse(DataBuf.empty().writeObject(response));
        }

        // change the local draining state
        case "change_draining_state" -> this.nodeServerProvider.localNode().drain(event.content().readBoolean());

        // none of our business
        default -> {
        }
      }
    }
  }
}
