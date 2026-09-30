/*
 * Copyright 2019-present CloudNetService team & contributors
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
import eu.cloudnetservice.node.cluster.NodeServer;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.cluster.NodeServerState;
import eu.cloudnetservice.node.impl.cluster.util.NodeDisconnectHandler;
import eu.cloudnetservice.utils.base.concurrent.TaskUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class NodeDisconnectTrackerTask implements Runnable {

  private static final Logger LOGGER = LoggerFactory.getLogger(NodeDisconnectTrackerTask.class);

  private static final long SOFT_DISCONNECT_MS_DELAY = Long.getLong("cloudnet.max.node.idle.millis", 30_000);
  // the time to wait for an established reconnect connection to complete the handshake before trying again
  private static final Duration RECONNECT_HANDSHAKE_TIMEOUT = Duration.ofSeconds(15);

  private final I18n i18n;
  private final NodeServerProvider provider;
  private final NodeDisconnectHandler disconnectHandler;
  private final Map<String, Instant> pendingReconnects = new ConcurrentHashMap<>();

  @Inject
  public NodeDisconnectTrackerTask(
    @NonNull @Service I18n i18n,
    @NonNull NodeServerProvider provider,
    @NonNull NodeDisconnectHandler disconnectHandler
  ) {
    this.i18n = i18n;
    this.provider = provider;
    this.disconnectHandler = disconnectHandler;
  }

  @Override
  public void run() {
    try {
      var currentTime = Instant.now();
      var local = this.provider.localNode();
      var hardDisconnectMillis = this.disconnectHandler.hardDisconnectMillis();
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
          this.disconnectHandler.markNodeServerDisconnected(server);
          // warn about that
          LOGGER.warn(this.i18n.translate("cluster-server-soft-disconnect", server.name(), updateDelay));
        }
      }

      // now check if a node is idling for ages and hard disconnect them
      for (var server : this.provider.nodeServers()) {
        // skip the local node and all nodes which aren't mark as disconnected (yet). nodes which are syncing are still in
        // the reconnect process and must be disconnected as well if the reconnect doesn't complete in time
        if (server == local) {
          continue;
        }
        var state = server.state();
        if (state != NodeServerState.DISCONNECTED && state != NodeServerState.SYNCING) {
          this.pendingReconnects.remove(server.name());
          continue;
        }

        // check if the node is exceeding the hard disconnect delay
        var disconnectMs = Duration.between(server.lastStateChange(), currentTime).toMillis();
        if (disconnectMs >= hardDisconnectMillis) {
          // close hard
          this.pendingReconnects.remove(server.name());
          server.close();
          LOGGER.warn(this.i18n.translate(
            "cluster-server-hard-disconnect",
            server.name(),
            hardDisconnectMillis,
            disconnectMs));
        } else if (state == NodeServerState.DISCONNECTED) {
          // check if we need to reconnect or if the other node is responsible to reconnect
          if (local.nodeInfoSnapshot().startupMillis() > server.nodeInfoSnapshot().startupMillis()) {
            this.tryReconnect(server, currentTime);
          }
        }
      }
    } catch (Exception exception) {
      LOGGER.error("Exception ticking node disconnect tracker", exception);
    }
  }

  private void tryReconnect(@NonNull NodeServer server, @NonNull Instant currentTime) {
    // the reconnect handshake can't handle multiple concurrent connections to the same node, don't open
    // another connection while the last established one might still be waiting for its handshake. a
    // state change after the last connection means that it either completed or failed
    var lastReconnect = this.pendingReconnects.get(server.name());
    if (lastReconnect != null
      && lastReconnect.isAfter(server.lastStateChange())
      && Duration.between(lastReconnect, currentTime).compareTo(RECONNECT_HANDSHAKE_TIMEOUT) < 0) {
      return;
    }

    // try to connect to the node server
    var connected = TaskUtil.getOrDefault(server.connect().thenApply(_ -> true), Duration.ofSeconds(5), false);
    if (connected) {
      this.pendingReconnects.put(server.name(), Instant.now());
    }
  }
}
