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

import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.wrapper.Wrapper;

public class PacketServerWrapperDriverAPIListener implements IPacketListener {

  @Override
  public void handle(INetworkChannel channel, IPacket packet) throws Exception {
    DriverAPIRequestType requestType = packet.getBuffer().readEnumConstant(DriverAPIRequestType.class);

    if (requestType == DriverAPIRequestType.FORCE_UPDATE_SERVICE) {
      ServiceInfoSnapshot serviceInfoSnapshot = Wrapper.getInstance().configureServiceInfoSnapshot();
      channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeObject(serviceInfoSnapshot)));
    }
  }

}
