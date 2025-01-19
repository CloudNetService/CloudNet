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

import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.cluster.NodeServerState;
import eu.cloudnetservice.node.impl.cluster.util.QueuedNetworkChannel;
import eu.cloudnetservice.utils.base.concurrent.TaskUtil;
import jakarta.inject.Singleton;
import java.time.Duration;
import java.time.Instant;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public record NodeDisconnectTrackerTask(@NonNull @Service I18n i18n, @NonNull NodeServerProvider provider) implements Runnable {

  private static final Logger LOGGER = LoggerFactory.getLogger(NodeDisconnectTrackerTask.class);

  private static final long SOFT_DISCONNECT_MS_DELAY = Long.getLong("cloudnet.max.node.idle.millis", 30_000);
  private static final long HARD_DISCONNECT_MS_DELAY = Long.getLong("cloudnet.max.node.disconnect.millis", 0);

  @Override
  public void run() {
    try {
      var currentTime = Instant.now();
      var local = this.provider.localNode();
      // first check all currently connected nodes if they are idling for too long
      for (var server : this.provider.nodeServers()) {
        // ignore the local node and all nodes which are not yet ready (these nodes do nothing which can lead to errors in
        // the cluster anyway)
        if (server == local || !server.available()) {
          continue;
        }

        // check if the server has been idling for too long
        var updateDelay = Duration.between(server.lastNodeInfoUpdate(), currentTime).toMillis();
        if (updateDelay >= SOFT_DISCONNECT_MS_DELAY) {
          // the node is idling for too long! Mark the node as disconnected and begin to schedule all packets to the node
          server.state(NodeServerState.DISCONNECTED);
          server.channel(new QueuedNetworkChannel(server.channel()));
          // trigger a head node refresh if the server is the head node to ensure that we're not using a head node which is dead
          if (this.provider.headNode().equals(server)) {
            this.provider.selectHeadNode();
          }
          // warn about that
          LOGGER.warn(this.i18n.translate("cluster-server-soft-disconnect", server.name(), updateDelay));
        }
      }

      // now check if a node is idling for ages and hard disconnect them
      for (var server : this.provider.nodeServers()) {
        // skip the local node and all nodes which aren't mark as disconnected (yet)
        if (server == local || server.state() != NodeServerState.DISCONNECTED) {
          continue;
        }

        // check if the node is exceeding the hard disconnect delay
        var disconnectMs = Duration.between(server.lastStateChange(), currentTime).toMillis();
        if (disconnectMs >= HARD_DISCONNECT_MS_DELAY) {
          // close hard
          server.close();
          LOGGER.warn(this.i18n.translate(
            "cluster-server-hard-disconnect",
            server.name(),
            HARD_DISCONNECT_MS_DELAY,
            disconnectMs));
        } else {
          // check if we need to reconnect or if the other node is responsible to reconnect
          if (local.nodeInfoSnapshot().startupMillis() > server.nodeInfoSnapshot().startupMillis()) {
            // try to connect to the node server
            TaskUtil.getOrDefault(server.connect(), Duration.ofSeconds(5), null);
          }
        }
      }
    } catch (Exception exception) {
      LOGGER.error("Exception ticking node disconnect tracker", exception);
    }
  }
}
