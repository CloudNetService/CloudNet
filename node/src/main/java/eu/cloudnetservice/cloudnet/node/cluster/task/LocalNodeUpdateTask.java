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

package eu.cloudnetservice.cloudnet.node.cluster.task;

import eu.cloudnetservice.cloudnet.common.log.LogManager;
import eu.cloudnetservice.cloudnet.common.log.Logger;
import eu.cloudnetservice.cloudnet.driver.channel.ChannelMessage;
import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBuf;
import eu.cloudnetservice.cloudnet.driver.network.def.NetworkConstants;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.cloudnet.node.cluster.NodeServerState;
import eu.cloudnetservice.cloudnet.node.cluster.defaults.DefaultNodeServerProvider;
import lombok.NonNull;

public record LocalNodeUpdateTask(@NonNull DefaultNodeServerProvider provider) implements Runnable {

  private static final Logger LOGGER = LogManager.logger(LocalNodeUpdateTask.class);

  @Override
  public void run() {
    try {
      var localNode = this.provider.localNode();
      var nodes = this.provider.availableNodeServers();
      // only publish the update if the local node is ready
      if (localNode.state() == NodeServerState.READY) {
        // update the local snapshot
        localNode.updateLocalSnapshot();
        // collect all nodes which are the target of the update
        // we include all remote nodes which are available and not the local node
        // we do this to explicitly trigger the disconnect handling on the other node if needed
        var targetNodes = nodes.stream()
          .filter(server -> server.state() == NodeServerState.READY)
          .filter(server -> server != localNode)
          .map(server -> server.info().uniqueId())
          .toList();
        if (!targetNodes.isEmpty()) {
          var message = ChannelMessage.builder()
            .message("update_node_info_snapshot")
            .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
            .buffer(DataBuf.empty().writeObject(localNode.nodeInfoSnapshot()))
            .prioritized(CloudNet.instance().mainThread().currentTick() % 10 == 0);
          // add all targets
          targetNodes.forEach(message::targetNode);
          // send the update to all active nodes
          message.build().send();
        }
      }
    } catch (Exception exception) {
      LOGGER.severe("Exception updating local node info to the cluster", exception);
    }
  }
}
