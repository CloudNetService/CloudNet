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

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.IEventManager;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceLifecycleChangeEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceLogEntryEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceUpdateEvent;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.def.NetworkConstants;
import de.dytanic.cloudnet.driver.provider.service.CloudServiceFactory;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.service.ICloudServiceManager;
import org.jetbrains.annotations.NotNull;

public final class ServiceChannelMessageListener {

  private final IEventManager eventManager;
  private final ICloudServiceManager serviceManager;
  private final CloudServiceFactory cloudServiceFactory;

  public ServiceChannelMessageListener(
    @NotNull IEventManager eventManager,
    @NotNull ICloudServiceManager serviceManager,
    @NotNull CloudServiceFactory cloudServiceFactory
  ) {
    this.eventManager = eventManager;
    this.serviceManager = serviceManager;
    this.cloudServiceFactory = cloudServiceFactory;
  }

  @EventListener
  public void handleChannelMessage(@NotNull ChannelMessageReceiveEvent event) {
    if (event.channel().equals(NetworkConstants.INTERNAL_MSG_CHANNEL)) {
      switch (event.message()) {
        // request to start a service
        case "node_to_head_start_service" -> {
          var configuration = event.content().readObject(ServiceConfiguration.class);
          event.binaryResponse(DataBuf.empty().writeObject(
              this.cloudServiceFactory.createCloudService(configuration)));
        }

        // request to start a service on the local node
        case "head_node_to_node_start_service" -> {
          var configuration = event.content().readObject(ServiceConfiguration.class);
          event.binaryResponse(DataBuf.empty().writeObject(
              this.serviceManager.createLocalCloudService(configuration).serviceInfo()));
        }

        // update of a service in the network
        case "update_service_info" -> {
          var snapshot = event.content().readObject(ServiceInfoSnapshot.class);
          // update locally and call the event
          this.serviceManager.handleServiceUpdate(snapshot, event.networkChannel());
          this.eventManager.callEvent(new CloudServiceUpdateEvent(snapshot));
        }

        // update of a service lifecycle in the network
        case "update_service_lifecycle" -> {
          var lifeCycle = event.content().readObject(ServiceLifeCycle.class);
          var snapshot = event.content().readObject(ServiceInfoSnapshot.class);
          // update locally and call the event
          this.serviceManager.handleServiceUpdate(snapshot, event.networkChannel());
          this.eventManager.callEvent(new CloudServiceLifecycleChangeEvent(lifeCycle, snapshot));
        }

        // call the event for a new line in the log of the service
        case "screen_new_line" -> {
          var snapshot = event.content().readObject(ServiceInfoSnapshot.class);
          var eventChannel = event.content().readString();
          var line = event.content().readString();

          this.eventManager.callEvent(eventChannel, new CloudServiceLogEntryEvent(snapshot, line));
        }

        // none of our business
        default -> {
        }
      }
    }
  }
}
