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

package eu.cloudnetservice.cloudnet.driver.network.def;

import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBuf;
import eu.cloudnetservice.cloudnet.driver.network.protocol.BasePacket;

public final class PacketClientAuthorization extends BasePacket {

  public PacketClientAuthorization(PacketAuthorizationType type, DataBuf dataBuf) {
    super(
      NetworkConstants.INTERNAL_AUTHORIZATION_CHANNEL,
      DataBuf.empty().writeObject(type).writeDataBuf(dataBuf));
  }

  public enum PacketAuthorizationType {

    NODE_TO_NODE,
    WRAPPER_TO_NODE
  }
}
