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

package eu.cloudnetservice.cloudnet.wrapper.network.listener.message;

import eu.cloudnetservice.cloudnet.driver.event.EventListener;
import eu.cloudnetservice.cloudnet.driver.event.EventManager;
import eu.cloudnetservice.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import eu.cloudnetservice.cloudnet.driver.event.events.service.CloudServiceLifecycleChangeEvent;
import eu.cloudnetservice.cloudnet.driver.event.events.service.CloudServiceLogEntryEvent;
import eu.cloudnetservice.cloudnet.driver.event.events.service.CloudServiceLogEntryEvent.StreamType;
import eu.cloudnetservice.cloudnet.driver.event.events.service.CloudServiceUpdateEvent;
import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBuf;
import eu.cloudnetservice.cloudnet.driver.network.def.NetworkConstants;
import eu.cloudnetservice.cloudnet.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.cloudnet.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.cloudnet.wrapper.Wrapper;
import lombok.NonNull;

public final class ServiceChannelMessageListener {

  private final EventManager eventManager;

  public ServiceChannelMessageListener(@NonNull EventManager eventManager) {
    this.eventManager = eventManager;
  }

  @EventListener
  public void handleChannelMessage(@NonNull ChannelMessageReceiveEvent event) {
    if (event.channel().equals(NetworkConstants.INTERNAL_MSG_CHANNEL)) {
      switch (event.message()) {
        // update of a service in the network
        case "update_service_info" -> {
          var snapshot = event.content().readObject(ServiceInfoSnapshot.class);
          // update locally and call the event
          this.eventManager.callEvent(new CloudServiceUpdateEvent(snapshot));
        }

        // update of a service lifecycle in the network
        case "update_service_lifecycle" -> {
          var lifeCycle = event.content().readObject(ServiceLifeCycle.class);
          var snapshot = event.content().readObject(ServiceInfoSnapshot.class);
          // update locally and call the event
          this.eventManager.callEvent(new CloudServiceLifecycleChangeEvent(lifeCycle, snapshot));
        }

        // force update request of the service info
        case "request_update_service_information" -> event.binaryResponse(DataBuf.empty()
          .writeObject(Wrapper.instance().configureServiceInfoSnapshot()));

        // call the event for a new line in the log of the service
        case "screen_new_line" -> {
          var snapshot = event.content().readObject(ServiceInfoSnapshot.class);
          var eventChannel = event.content().readString();
          var line = event.content().readString();
          var type = event.content().readBoolean() ? StreamType.STDERR : StreamType.STDOUT;

          this.eventManager.callEvent(eventChannel, new CloudServiceLogEntryEvent(snapshot, line, type));
        }

        // none of our business
        default -> {
        }
      }
    }
  }
}
