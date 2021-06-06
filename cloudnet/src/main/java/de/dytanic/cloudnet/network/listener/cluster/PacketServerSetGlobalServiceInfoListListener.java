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

package de.dytanic.cloudnet.network.listener.cluster;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerServiceInfoPublisher;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import java.util.Collection;

public final class PacketServerSetGlobalServiceInfoListListener implements IPacketListener {

  @Override
  public void handle(INetworkChannel channel, IPacket packet) {
    Collection<ServiceInfoSnapshot> serviceInfoSnapshots = packet.getBuffer()
      .readObjectCollection(ServiceInfoSnapshot.class);

    for (ServiceInfoSnapshot serviceInfoSnapshot : serviceInfoSnapshots) {
      if (serviceInfoSnapshot != null) {
        CloudNet.getInstance().getCloudServiceManager()
          .handleServiceUpdate(PacketClientServerServiceInfoPublisher.PublisherType.REGISTER, serviceInfoSnapshot);
      }
    }
  }
}
