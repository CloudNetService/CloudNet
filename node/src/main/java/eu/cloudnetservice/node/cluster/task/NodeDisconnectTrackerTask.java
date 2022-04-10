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

package eu.cloudnetservice.node.cluster.task;

import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.cluster.NodeServerState;
import eu.cloudnetservice.node.cluster.util.QueuedNetworkChannel;
import java.time.Duration;
import java.time.Instant;
import lombok.NonNull;

public record NodeDisconnectTrackerTask(@NonNull NodeServerProvider provider) implements Runnable {

  private static final Logger LOGGER = LogManager.logger(NodeDisconnectTrackerTask.class);

  private static final long SOFT_DISCONNECT_MS_DELAY = Long.getLong("cloudnet.max.node.idle.millis", 30_000);
  private static final long HARD_DISCONNECT_MS_DELAY = Long.getLong("cloudnet.max.node.disconnect.millis", 0);

  @Override
  public void run() {
    try {
      var local = this.provider.localNode();
      // first check all currently connected nodes if they are idling for too long
      for (var server : this.provider.nodeServers()) {
        // ignore the local node and all nodes which are not yet ready (these nodes do nothing which can lead to errors in
        // the cluster anyway)
        if (server == local || !server.available()) {
          continue;
        }
        // check if the server has been idling for too long
        var updateDelay = System.currentTimeMillis() - server.nodeInfoSnapshot().creationTime();
        if (updateDelay >= SOFT_DISCONNECT_MS_DELAY) {
          // the node is idling for too long! Mark the node as disconnected and begin to schedule all packets to the node
          server.state(NodeServerState.DISCONNECTED);
          server.channel(new QueuedNetworkChannel(server.channel()));
          // trigger a head node refresh if the server is the head node to ensure that we're not using a head node which is dead
          if (this.provider.headNode() == server) {
            this.provider.selectHeadNode();
          }
          // warn about that
          LOGGER.warning(I18n.trans("cluster-server-soft-disconnect", server.name(), updateDelay));
        }
      }

      // now check if a node is idling for ages and hard disconnect them
      for (var server : this.provider.nodeServers()) {
        // skip the local node and all nodes which aren't mark as disconnected (yet)
        if (server == local || server.state() != NodeServerState.DISCONNECTED) {
          continue;
        }
        // check if the node is exceeding the hard disconnect delay
        var disconnectMs = Duration.between(server.lastStateChangeStamp(), Instant.now()).toMillis();
        if (disconnectMs >= HARD_DISCONNECT_MS_DELAY) {
          // close hard
          server.close();
          LOGGER.warning(I18n.trans(
            "cluster-server-hard-disconnect",
            server.name(),
            HARD_DISCONNECT_MS_DELAY,
            disconnectMs));
        } else {
          // check if we need to reconnect or if the other node is responsible to reconnect
          if (local.nodeInfoSnapshot().startupMillis() > server.nodeInfoSnapshot().startupMillis()) {
            // try to connect to the node server
            server.connect();
          }
        }
      }
    } catch (Exception exception) {
      LOGGER.severe("Exception ticking node disconnect tracker", exception);
    }
  }
}
