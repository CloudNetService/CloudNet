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

package eu.cloudnetservice.node.impl.cluster.task;

import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.impl.network.NetworkConstants;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.cluster.NodeServerState;
import eu.cloudnetservice.node.impl.tick.DefaultTickLoop;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public record LocalNodeUpdateTask(
  @NonNull NodeServerProvider provider,
  @NonNull Provider<DefaultTickLoop> mainThreadProvider
) implements Runnable {

  private static final Logger LOGGER = LoggerFactory.getLogger(LocalNodeUpdateTask.class);

  @Override
  public void run() {
    try {
      var localNode = this.provider.localNode();
      var nodes = this.provider.nodeServers();
      // only publish the update if the local node is ready
      if (localNode.state() == NodeServerState.READY) {
        // update the local snapshot
        localNode.updateLocalSnapshot();
        // collect all nodes which are the target of the update
        // we include all remote nodes which are available and not the local node
        // we do this to explicitly trigger the disconnect handling on the other node if needed
        var targetNodes = nodes.stream()
          .filter(server -> server != localNode)
          // we can not use NodeServer#available here as it also verifies that the node has already
          // exchanged a node snapshot which must not be the case (as this task will trigger the
          // initial exchange of a node snapshot)
          .filter(server -> server.state() == NodeServerState.READY)
          .map(server -> server.info().uniqueId())
          .toList();
        if (!targetNodes.isEmpty()) {
          var message = ChannelMessage.builder()
            .sendSync(true) // ensure that we don't schedule too many updates while other are still waiting
            .message("update_node_info_snapshot")
            .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
            .buffer(DataBuf.empty().writeObject(localNode.nodeInfoSnapshot()))
            .prioritized(this.mainThreadProvider.get().currentTick() % 10 == 0);
          // add all targets
          targetNodes.forEach(message::targetNode);
          // send the update to all active nodes
          message.build().send();
        }
      }
    } catch (Exception exception) {
      LOGGER.error("Exception updating local node info to the cluster", exception);
    }
  }
}
