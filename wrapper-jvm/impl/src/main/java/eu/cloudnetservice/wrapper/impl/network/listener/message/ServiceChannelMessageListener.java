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

package eu.cloudnetservice.wrapper.impl.network.listener.message;

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.event.events.channel.ChannelMessageReceiveEvent;
import eu.cloudnetservice.driver.event.events.service.CloudServiceDeferredStateEvent;
import eu.cloudnetservice.driver.event.events.service.CloudServiceLifecycleChangeEvent;
import eu.cloudnetservice.driver.event.events.service.CloudServiceLogEntryEvent;
import eu.cloudnetservice.driver.event.events.service.CloudServiceUpdateEvent;
import eu.cloudnetservice.driver.impl.network.NetworkConstants;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.service.ServiceCreateResult;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.wrapper.holder.ServiceInfoHolder;
import lombok.NonNull;

public final class ServiceChannelMessageListener {

  @EventListener
  public void handleChannelMessage(
    @NonNull ChannelMessageReceiveEvent event,
    @NonNull EventManager eventManager,
    @NonNull ServiceInfoHolder serviceInfoHolder
  ) {
    if (event.channel().equals(NetworkConstants.INTERNAL_MSG_CHANNEL)) {
      switch (event.message()) {
        // update of a service in the network
        case "update_service_info" -> {
          var snapshot = event.content().readObject(ServiceInfoSnapshot.class);
          // update locally and call the event
          eventManager.callEvent(new CloudServiceUpdateEvent(snapshot));
        }

        // update of a service lifecycle in the network
        case "update_service_lifecycle" -> {
          var lifeCycle = event.content().readObject(ServiceLifeCycle.class);
          var snapshot = event.content().readObject(ServiceInfoSnapshot.class);
          // update locally and call the event
          eventManager.callEvent(new CloudServiceLifecycleChangeEvent(lifeCycle, snapshot));
        }

        // force update request of the service info
        case "request_update_service_information" -> event.binaryResponse(DataBuf.empty()
          .writeObject(serviceInfoHolder.configureServiceInfoSnapshot()));

        // force update request of the service information with new properties
        case "request_update_service_information_with_new_properties" -> {
          var properties = event.content().readObject(Document.class);
          var snapshot = serviceInfoHolder.createServiceInfoSnapshot(properties);

          // publish the new service info
          serviceInfoHolder.publishServiceInfoUpdate(snapshot);
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
}
