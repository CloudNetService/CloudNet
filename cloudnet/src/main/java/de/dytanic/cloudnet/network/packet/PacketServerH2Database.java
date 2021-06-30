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

package de.dytanic.cloudnet.network.packet;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;

public final class PacketServerH2Database extends Packet {

  public PacketServerH2Database(OperationType operationType, String name, String key, JsonDocument document) {
    super(PacketConstants.INTERNAL_H2_DATABASE_UPDATE_MODULE, new JsonDocument("operationType", operationType)
        .append("name", name)
        .append("key", key)
        .append("document", document),
      new byte[0]);
  }

  public enum OperationType {
    INSERT,
    UPDATE,
    DELETE,
    CLEAR
  }
}
