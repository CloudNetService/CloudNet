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

package eu.cloudnetservice.driver.impl.network.rpc;

import eu.cloudnetservice.driver.impl.network.NetworkConstants;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.protocol.BasePacket;
import lombok.NonNull;

/**
 * The packet used for any rpc within the CloudNet network. This packet should only be used internally, and any
 * developer should use the rpc api rather than sending this packet themselves.
 *
 * @since 4.0
 */
public class RPCRequestPacket extends BasePacket {

  /**
   * Constructs a new rpc request packet.
   *
   * @param dataBuf the encoded data of the rpc, ready for interpretation and handling on the receiver site.
   * @throws NullPointerException if the given data buffer is null.
   */
  public RPCRequestPacket(@NonNull DataBuf dataBuf) {
    super(NetworkConstants.INTERNAL_RPC_COM_CHANNEL, dataBuf);
  }
}
