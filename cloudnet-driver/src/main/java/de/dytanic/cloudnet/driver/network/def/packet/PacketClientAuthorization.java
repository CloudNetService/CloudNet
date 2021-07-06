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

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;

public final class PacketClientAuthorization extends Packet {

  public PacketClientAuthorization(PacketAuthorizationType packetAuthorizationType, JsonDocument credentials) {
    super(PacketConstants.INTERNAL_AUTHORIZATION_CHANNEL, new JsonDocument());

    Preconditions.checkNotNull(packetAuthorizationType);
    Preconditions.checkNotNull(credentials);

    this.header.append("authorization", packetAuthorizationType).append("credentials", credentials);
  }

  public enum PacketAuthorizationType {

    NODE_TO_NODE(0),
    WRAPPER_TO_NODE(1);

    private final int value;

    PacketAuthorizationType(int value) {
      this.value = value;
    }

    public int getValue() {
      return this.value;
    }
  }
}
