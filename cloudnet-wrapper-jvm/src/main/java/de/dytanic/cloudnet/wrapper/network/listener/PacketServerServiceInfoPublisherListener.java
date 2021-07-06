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

package de.dytanic.cloudnet.wrapper.network.listener;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceConnectNetworkEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceDisconnectNetworkEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceInfoUpdateEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceRegisterEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceStartEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceStopEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceUnregisterEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerServiceInfoPublisher;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

public final class PacketServerServiceInfoPublisherListener implements IPacketListener {

  @Override
  public void handle(INetworkChannel channel, IPacket packet) {
    ServiceInfoSnapshot serviceInfoSnapshot = packet.getBuffer().readObject(ServiceInfoSnapshot.class);
    PacketClientServerServiceInfoPublisher.PublisherType publisherType = packet.getBuffer()
      .readEnumConstant(PacketClientServerServiceInfoPublisher.PublisherType.class);

    switch (publisherType) {
      case UPDATE:
        this.invoke0(new CloudServiceInfoUpdateEvent(serviceInfoSnapshot));
        break;
      case REGISTER:
        this.invoke0(new CloudServiceRegisterEvent(serviceInfoSnapshot));
        break;
      case CONNECTED:
        this.invoke0(new CloudServiceConnectNetworkEvent(serviceInfoSnapshot));
        break;
      case UNREGISTER:
        this.invoke0(new CloudServiceUnregisterEvent(serviceInfoSnapshot));
        break;
      case DISCONNECTED:
        this.invoke0(new CloudServiceDisconnectNetworkEvent(serviceInfoSnapshot));
        break;
      case STARTED:
        this.invoke0(new CloudServiceStartEvent(serviceInfoSnapshot));
        break;
      case STOPPED:
        this.invoke0(new CloudServiceStopEvent(serviceInfoSnapshot));
        break;
      default:
        break;
    }
  }

  private void invoke0(Event event) {
    CloudNetDriver.getInstance().getEventManager().callEvent(event);
  }
}
