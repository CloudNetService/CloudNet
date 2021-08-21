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
import de.dytanic.cloudnet.driver.network.NetworkUpdateType;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import java.util.Map;

public final class PacketServerSetLocalDatabaseData extends Packet {

  public PacketServerSetLocalDatabaseData(Map<String, Map<String, JsonDocument>> documents, NetworkUpdateType type) {
    //TODO:  super(PacketConstants.INTERNAL_LOCAL_DATABASE_SET_DATA_CHANNEL, JsonDocument.newDocument("documents", documents)
    //TODO:  .append("updateType", type));
  }
}
