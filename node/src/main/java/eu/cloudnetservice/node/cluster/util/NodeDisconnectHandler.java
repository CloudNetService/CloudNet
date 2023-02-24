/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.node.cluster.util;

import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.channel.ChannelMessageTarget;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.event.events.service.CloudServiceLifecycleChangeEvent;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.def.NetworkConstants;
import eu.cloudnetservice.driver.service.ProcessSnapshot;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.node.cluster.NodeServer;
import eu.cloudnetservice.node.service.CloudService;
import eu.cloudnetservice.node.service.CloudServiceManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Collection;
import lombok.NonNull;

@Singleton
public final class NodeDisconnectHandler {

  private static final Logger LOGGER = LogManager.logger(NodeDisconnectHandler.class);

  private final EventManager eventManager;
  private final CloudServiceManager serviceManager;

  @Inject
  public NodeDisconnectHandler(@NonNull EventManager eventManager, @NonNull CloudServiceManager serviceManager) {
    this.eventManager = eventManager;
    this.serviceManager = serviceManager;
  }

  private static @NonNull ChannelMessage.Builder targetServices(@NonNull Collection<CloudService> services) {
    var builder = ChannelMessage.builder();
    // iterate over all local services - if the service is connected append it as target
    for (var service : services) {
      builder.target(ChannelMessageTarget.Type.SERVICE, service.serviceId().name());
    }
    // for chaining
    return builder;
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
            .buffer(DataBuf.empty().writeObject(lifeCycle).writeObject(newSnapshot))
            .build()
            .send();
        }
      }
    }

    LOGGER.info(I18n.trans("cluster-server-networking-disconnected", server.name()));
  }
}
