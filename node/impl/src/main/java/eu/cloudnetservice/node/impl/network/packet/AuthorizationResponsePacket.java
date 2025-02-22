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

package eu.cloudnetservice.node.impl.network.packet;

import eu.cloudnetservice.driver.impl.network.NetworkConstants;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.protocol.BasePacket;
import org.jetbrains.annotations.Nullable;

public final class AuthorizationResponsePacket extends BasePacket {

  public AuthorizationResponsePacket(boolean success, boolean reconnect, @Nullable DataBuf extraData) {
    super(
      NetworkConstants.INTERNAL_AUTHORIZATION_CHANNEL,
      DataBuf.empty()
        .writeBoolean(success)
        .writeBoolean(reconnect)
        .writeObject(extraData));
  }
}
