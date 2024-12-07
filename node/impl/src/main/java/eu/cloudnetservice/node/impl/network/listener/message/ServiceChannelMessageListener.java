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

package eu.cloudnetservice.node.impl.network.listener.message;

import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.event.events.channel.ChannelMessageReceiveEvent;
import eu.cloudnetservice.driver.event.events.service.CloudServiceDeferredStateEvent;
import eu.cloudnetservice.driver.event.events.service.CloudServiceLifecycleChangeEvent;
import eu.cloudnetservice.driver.event.events.service.CloudServiceLogEntryEvent;
import eu.cloudnetservice.driver.event.events.service.CloudServiceUpdateEvent;
import eu.cloudnetservice.driver.impl.network.NetworkConstants;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.provider.CloudServiceFactory;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.driver.service.ServiceCreateResult;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.node.impl.service.InternalCloudServiceManager;
import jakarta.inject.Singleton;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class ServiceChannelMessageListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServiceChannelMessageListener.class);

  @EventListener
  public void handleChannelMessage(
    @NonNull ChannelMessageReceiveEvent event,
    @NonNull EventManager eventManager,
    @NonNull InternalCloudServiceManager serviceManager,
    @NonNull CloudServiceFactory cloudServiceFactory
  ) {
    if (event.channel().equals(NetworkConstants.INTERNAL_MSG_CHANNEL)) {
      switch (event.message()) {
        // request to start a service
        case "node_to_head_start_service" -> {
          var configuration = event.content().readObject(ServiceConfiguration.class);
          event.queryResponse(cloudServiceFactory.createCloudServiceAsync(configuration)
            .thenApply(service -> ChannelMessage.buildResponseFor(event.channelMessage())
              .buffer(DataBuf.empty().writeObject(service))
              .build()));
        }

        // feedback from a node that a service which should have been moved to accepted
        // is no longer registered as unaccepted and not yet moved to registered, which
        // means that the cache ttl on the target node exceeded
        case "node_to_head_node_unaccepted_service_ttl_exceeded" -> {
          var serviceUniqueId = event.content().readUniqueId();
          serviceManager.forceRemoveRegisteredService(serviceUniqueId);
        }

        // request to start a service on the local node
        case "head_node_to_node_start_service" -> {
          var configuration = event.content().readObject(ServiceConfiguration.class);
          var service = serviceManager.createLocalCloudService(configuration);

          event.binaryResponse(DataBuf.empty().writeObject(ServiceCreateResult.created(service.serviceInfo())));
        }

        // publish the service info of a created service to the cluster
        case "head_node_to_node_finish_service_registration" -> {
          var serviceUniqueId = event.content().readUniqueId();
          var service = serviceManager.takeUnacceptedService(serviceUniqueId);

          if (service != null) {
            // service is still locally present, finish the registration of it
            service.handleServiceRegister();
          } else {
            // service is no longer locally present as unaccepted
            // re-check if the service was already moved to registered
            var registeredService = serviceManager.localCloudService(serviceUniqueId);
            if (registeredService == null) {
              // send this as feedback to the head node in order to remove the registered service from there as well
              ChannelMessage.builder()
                .target(event.sender().toTarget())
                .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
                .message("node_to_head_node_unaccepted_service_ttl_exceeded")
                .buffer(DataBuf.empty().writeUniqueId(serviceUniqueId))
                .build()
                .send();
            }
          }
        }

        // update of a service in the network
        case "update_service_info" -> {
          var snapshot = event.content().readObject(ServiceInfoSnapshot.class);
          // update locally and call the event
          serviceManager.handleServiceUpdate(snapshot, event.networkChannel());
          eventManager.callEvent(new CloudServiceUpdateEvent(snapshot));
        }

        // update of a service lifecycle in the network
        case "update_service_lifecycle" -> {
          var lifeCycle = event.content().readObject(ServiceLifeCycle.class);
          var snapshot = event.content().readObject(ServiceInfoSnapshot.class);
          // update locally and call the event
          serviceManager.handleServiceUpdate(snapshot, event.networkChannel());
          eventManager.callEvent(new CloudServiceLifecycleChangeEvent(lifeCycle, snapshot));
        }

        // call the event for a new line in the log of the service
        case "screen_new_line" -> {
          var snapshot = event.content().readObject(ServiceInfoSnapshot.class);
          var eventChannel = event.content().readString();
          var line = event.content().readString();
          var type = event.content().readBoolean()
            ? CloudServiceLogEntryEvent.StreamType.STDERR
            : CloudServiceLogEntryEvent.StreamType.STDOUT;

          eventManager.callEvent(eventChannel, new CloudServiceLogEntryEvent(snapshot, line, type));
        }

        // a deferred service start result is available, call the event
        case "deferred_service_event" -> {
          var creationId = event.content().readUniqueId();
          var createResult = event.content().readObject(ServiceCreateResult.class);

          eventManager.callEvent(new CloudServiceDeferredStateEvent(creationId, createResult));
        }

        // none of our business
        default -> {
        }
      }
    }
  }

  @EventListener
  public void handleRemoteLifecycleChanges(@NonNull CloudServiceLifecycleChangeEvent event, @NonNull @Service I18n i18n) {
    var id = event.serviceInfo().serviceId();
    var replacements = new Object[]{id.uniqueId(), id.taskName(), id.name(), id.nodeUniqueId()};

    switch (event.newLifeCycle()) {
      case RUNNING -> LOGGER.info(i18n.translate("cloudnet-service-post-start-message-different-node", replacements));
      case STOPPED -> LOGGER.info(i18n.translate("cloudnet-service-post-stop-message-different-node", replacements));
      default -> {
      }
    }
  }
}
