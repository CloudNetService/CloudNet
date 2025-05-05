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

package eu.cloudnetservice.node.impl.cluster.util;

import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.channel.ChannelMessageTarget;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.event.events.service.CloudServiceLifecycleChangeEvent;
import eu.cloudnetservice.driver.impl.network.NetworkConstants;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.driver.service.ProcessSnapshot;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.node.cluster.NodeServer;
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

  @Inject
  public NodeDisconnectHandler(
    @NonNull @Service I18n i18n,
    @NonNull EventManager eventManager,
    @NonNull InternalCloudServiceManager serviceManager
  ) {
    this.i18n = i18n;
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

    LOGGER.info(this.i18n.translate("cluster-server-networking-disconnected", server.name()));
  }
}
