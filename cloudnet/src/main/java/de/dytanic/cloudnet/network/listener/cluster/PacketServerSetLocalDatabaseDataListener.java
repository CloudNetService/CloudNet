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

package de.dytanic.cloudnet.network.listener.cluster;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.LocalDatabase;
import de.dytanic.cloudnet.driver.database.DatabaseProvider;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import java.lang.reflect.Type;
import java.util.Map;

public final class PacketServerSetLocalDatabaseDataListener implements IPacketListener {

  private static final Type TYPE = TypeToken.getParameterized(Map.class, String.class,
    TypeToken.getParameterized(Map.class, String.class, JsonDocument.class).getType()).getType();

  @Override
  public void handle(INetworkChannel channel, IPacket packet) {
    if (CloudNet.getInstance().getDatabaseProvider().needsClusterSync()) {
      Map<String, Map<String, JsonDocument>> documents = packet.getHeader().get("documents", TYPE);

      DatabaseProvider databaseProvider = CloudNet.getInstance().getDatabaseProvider();
      for (String name : databaseProvider.getDatabaseNames()) {
        databaseProvider.deleteDatabase(name);
      }

      for (Map.Entry<String, Map<String, JsonDocument>> entry : documents.entrySet()) {
        LocalDatabase database = (LocalDatabase) databaseProvider.getDatabase(entry.getKey());

        for (Map.Entry<String, JsonDocument> keyValuePair : documents.get(entry.getKey()).entrySet()) {
          database.insertWithoutHandlerCall(keyValuePair.getKey(), keyValuePair.getValue());
        }
      }

      for (Map.Entry<String, Map<String, JsonDocument>> entry : documents.entrySet()) {
        entry.getValue().clear();
      }

      documents.clear();
    }
  }
}
