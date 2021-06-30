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

import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import java.util.function.Consumer;

public class PacketClientDriverAPI extends Packet {

  public PacketClientDriverAPI(DriverAPIRequestType type) {
    this(type, null);
  }

  public PacketClientDriverAPI(DriverAPIRequestType type, Consumer<ProtocolBuffer> modifier) {
    super(PacketConstants.INTERNAL_DRIVER_API_CHANNEL, ProtocolBuffer.create().writeEnumConstant(type));
    if (modifier != null) {
      modifier.accept(super.body);
    }
  }

}
