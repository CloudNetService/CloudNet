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
import de.dytanic.cloudnet.driver.event.events.service.task.ServiceTaskAddEvent;
import de.dytanic.cloudnet.driver.event.events.service.task.ServiceTaskRemoveEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.NetworkUpdateType;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import java.util.ArrayList;
import java.util.List;

public final class PacketServerSetServiceTaskListListener implements IPacketListener {

  @Override
  public void handle(INetworkChannel channel, IPacket packet) {
    List<ServiceTask> serviceTasks = new ArrayList<>(packet.getBuffer().readObjectCollection(ServiceTask.class));
    NetworkUpdateType updateType = packet.getBuffer().readEnumConstant(NetworkUpdateType.class);

    if (updateType == null) {
      return;
    }

    switch (updateType) {
      case ADD:
        for (ServiceTask serviceTask : serviceTasks) {
          CloudNetDriver.getInstance().getEventManager().callEvent(new ServiceTaskAddEvent(serviceTask));
        }
        break;
      case REMOVE:
        for (ServiceTask serviceTask : serviceTasks) {
          CloudNetDriver.getInstance().getEventManager().callEvent(new ServiceTaskRemoveEvent(serviceTask));
        }
        break;
      default:
        break;
    }
  }

}
