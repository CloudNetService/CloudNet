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

package de.dytanic.cloudnet.driver.network.def.packet;

import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

public final class PacketClientServerServiceInfoPublisher extends Packet {

  public PacketClientServerServiceInfoPublisher(ServiceInfoSnapshot serviceInfoSnapshot, PublisherType publisherType) {
    super(PacketConstants.SERVICE_INFO_PUBLISH_CHANNEL,
      ProtocolBuffer.create().writeObject(serviceInfoSnapshot).writeEnumConstant(publisherType));
  }

  public enum PublisherType {
    UPDATE,
    STARTED,
    STOPPED,
    CONNECTED,
    DISCONNECTED,
    UNREGISTER,
    REGISTER
  }
}
