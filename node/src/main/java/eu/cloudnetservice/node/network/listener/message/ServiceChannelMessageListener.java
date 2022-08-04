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

package eu.cloudnetservice.node.network.listener.message;

import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.event.events.channel.ChannelMessageReceiveEvent;
import eu.cloudnetservice.driver.event.events.service.CloudServiceDeferredStateEvent;
import eu.cloudnetservice.driver.event.events.service.CloudServiceLifecycleChangeEvent;
import eu.cloudnetservice.driver.event.events.service.CloudServiceLogEntryEvent;
import eu.cloudnetservice.driver.event.events.service.CloudServiceUpdateEvent;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.def.NetworkConstants;
import eu.cloudnetservice.driver.provider.CloudServiceFactory;
import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.driver.service.ServiceCreateResult;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.node.service.CloudServiceManager;
import lombok.NonNull;

public final class ServiceChannelMessageListener {

  private static final Logger LOGGER = LogManager.logger(ServiceChannelMessageListener.class);

  private final EventManager eventManager;
  private final CloudServiceManager serviceManager;
  private final CloudServiceFactory cloudServiceFactory;

  public ServiceChannelMessageListener(
    @NonNull EventManager eventManager,
    @NonNull CloudServiceManager serviceManager,
    @NonNull CloudServiceFactory cloudServiceFactory
  ) {
    this.eventManager = eventManager;
    this.serviceManager = serviceManager;
    this.cloudServiceFactory = cloudServiceFactory;
  }

  @EventListener
  public void handleChannelMessage(@NonNull ChannelMessageReceiveEvent event) {
    if (event.channel().equals(NetworkConstants.INTERNAL_MSG_CHANNEL)) {
      switch (event.message()) {
        // request to start a service
        case "node_to_head_start_service" -> {
          var configuration = event.content().readObject(ServiceConfiguration.class);
          event.queryResponse(this.cloudServiceFactory.createCloudServiceAsync(configuration)
            .thenApply(service -> ChannelMessage.buildResponseFor(event.channelMessage())
              .buffer(DataBuf.empty().writeObject(service))
              .build()));
        }

        // request to start a service on the local node
        case "head_node_to_node_start_service" -> {
          var configuration = event.content().readObject(ServiceConfiguration.class);
          var service = this.serviceManager.createLocalCloudService(configuration);

          event.binaryResponse(DataBuf.empty().writeObject(ServiceCreateResult.created(service.serviceInfo())));
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
          var type = event.content().readBoolean()
            ? CloudServiceLogEntryEvent.StreamType.STDERR
            : CloudServiceLogEntryEvent.StreamType.STDOUT;

          this.eventManager.callEvent(eventChannel, new CloudServiceLogEntryEvent(snapshot, line, type));
        }

        // a deferred service start result is available, call the event
        case "deferred_service_event" -> {
          var creationId = event.content().readUniqueId();
          var createResult = event.content().readObject(ServiceCreateResult.class);

          this.eventManager.callEvent(new CloudServiceDeferredStateEvent(creationId, createResult));
        }

        // none of our business
        default -> {
        }
      }
    }
  }

  @EventListener
  public void handleRemoteLifecycleChanges(@NonNull CloudServiceLifecycleChangeEvent event) {
    var id = event.serviceInfo().serviceId();
    var replacements = new Object[]{id.uniqueId(), id.taskName(), id.name(), id.nodeUniqueId()};

    switch (event.newLifeCycle()) {
      case RUNNING -> LOGGER.info(I18n.trans("cloudnet-service-post-start-message-different-node", replacements));
      case STOPPED -> LOGGER.info(I18n.trans("cloudnet-service-post-stop-message-different-node", replacements));
      default -> {
      }
    }
  }
}
