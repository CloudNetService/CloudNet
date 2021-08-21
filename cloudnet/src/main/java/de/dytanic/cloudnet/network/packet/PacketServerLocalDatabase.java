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
import de.dytanic.cloudnet.driver.network.protocol.Packet;

public final class PacketServerLocalDatabase extends Packet {

  public PacketServerLocalDatabase(OperationType operationType, String name, String key, JsonDocument document) {
    //TODO:  super(PacketConstants.INTERNAL_LOCAL_DATABASE_SYNC_CHANNEL, JsonDocument.newDocument("operationType", operationType)
    //TODO:   .append("name", name)
    //TODO:   .append("key", key)
    //TODO:   .append("document", document));
  }

  public enum OperationType {
    INSERT,
    UPDATE,
    DELETE,
    CLEAR
  }
}
