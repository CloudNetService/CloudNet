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

package eu.cloudnetservice.node.impl.cluster.util;

import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.event.events.service.CloudServiceLifecycleChangeEvent;
import eu.cloudnetservice.driver.impl.network.NetworkConstants;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.driver.service.ProcessSnapshot;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.node.cluster.NodeServer;
import eu.cloudnetservice.node.cluster.NodeServerState;
import eu.cloudnetservice.node.impl.service.InternalCloudServiceManager;
import eu.cloudnetservice.node.service.CloudService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Collection;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class NodeDisconnectHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(NodeDisconnectHandler.class);

  private final I18n i18n;
  private final EventManager eventManager;
  private final InternalCloudServiceManager serviceManager;
  private final long hardDisconnectMillis;

  @Inject
  public NodeDisconnectHandler(
    @NonNull @Service I18n i18n,
    @NonNull EventManager eventManager,
    @NonNull InternalCloudServiceManager serviceManager
  ) {
    this.i18n = i18n;
    this.eventManager = eventManager;
    this.serviceManager = serviceManager;
    this.hardDisconnectMillis = Long.getLong("cloudnet.max.node.disconnect.millis", 0);
  }

  private static @NonNull ChannelMessage.Builder targetServices(@NonNull Collection<CloudService> services) {
    var builder = ChannelMessage.builder();
    // iterate over all local services - if the service is connected append it as target
    for (var service : services) {
      builder.targetService(service.serviceId().name());
    }
    // for chaining
    return builder;
  }

  /**
   * Gets the time in milliseconds a node gets to reconnect after it was marked as disconnected, before it gets removed
   * from the cluster. A value of 0 means that nodes are removed from the cluster instantly. The value is configured
   * using the {@code cloudnet.max.node.disconnect.millis} system property.
   *
   * @return the reconnect grace period of nodes in milliseconds.
   * @since 4.0
   */
  public long hardDisconnectMillis() {
    return this.hardDisconnectMillis;
  }

  /**
   * Marks the given node server as disconnected. All packets sent to the node are queued until the node reconnects,
   * and a new head node is selected if the given node was the head node. The node disconnect tracker takes care of
   * reconnecting to the node or removing it from the cluster if it does not reconnect in time.
   *
   * @param server the node server to mark as disconnected.
   * @throws NullPointerException if the given server is null.
   * @since 4.0
   */
  public void markNodeServerDisconnected(@NonNull NodeServer server) {
    // mark the node as disconnected and begin to schedule all packets to the node until it reconnected
    server.state(NodeServerState.DISCONNECTED);
    server.channel(new QueuedNetworkChannel(server.channel()));
    // trigger a head node refresh if the server is the head node to ensure that we're not using a head node which is dead
    if (server.head()) {
      server.provider().selectHeadNode();
    }
  }

  /**
   * Handles the close of the network channel of the given node server. If a reconnect grace period is configured and
   * the node is available, the node is marked as disconnected to give it the chance to reconnect. In all other cases
   * the node is closed and removed from the cluster instantly.
   *
   * @param server the node server whose network channel was closed.
   * @throws NullPointerException if the given server is null.
   * @since 4.0
   */
  public void handleNodeServerChannelClose(@NonNull NodeServer server) {
    // give the node the chance to reconnect if a grace period is configured, the disconnect tracker takes
    // care of the reconnect and removes the node from the cluster if it did not reconnect in time
    if (this.hardDisconnectMillis > 0 && server.available()) {
      this.markNodeServerDisconnected(server);
      LOGGER.warn(this.i18n.translate("cluster-server-connection-lost", server.name(), this.hardDisconnectMillis));
    } else {
      server.close();
    }
  }

  public void handleNodeServerClose(@NonNull NodeServer server) {
    for (var snapshot : this.serviceManager.services()) {
      if (snapshot.serviceId().nodeUniqueId().equalsIgnoreCase(server.name())) {
        // rebuild the service snapshot with a DELETED state
        var lifeCycle = snapshot.lifeCycle();
        var newSnapshot = new ServiceInfoSnapshot(
          System.currentTimeMillis(),
          snapshot.address(),
          ProcessSnapshot.empty(),
          snapshot.configuration(),
          -1,
          ServiceLifeCycle.DELETED,
          snapshot.propertyHolder());

        // publish the update to the local service manager & call the local change event
        this.serviceManager.handleServiceUpdate(newSnapshot, null);
        this.eventManager.callEvent(new CloudServiceLifecycleChangeEvent(lifeCycle, newSnapshot));

        // send the change to all service - all other nodes will handle the close as well (if there are any)
        var localServices = this.serviceManager.localCloudServices();
        if (!localServices.isEmpty()) {
          targetServices(localServices)
            .message("update_service_lifecycle")
            .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
            .build(buffer -> buffer.writeObject(lifeCycle).writeObject(newSnapshot))
            .send();
        }
      }
    }

    LOGGER.info(this.i18n.translate("cluster-server-networking-disconnected", server.name()));
  }
}
