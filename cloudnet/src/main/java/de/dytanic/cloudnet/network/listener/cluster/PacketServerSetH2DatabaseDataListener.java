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
import de.dytanic.cloudnet.database.h2.H2Database;
import de.dytanic.cloudnet.database.h2.H2DatabaseProvider;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import java.lang.reflect.Type;
import java.util.Map;

public final class PacketServerSetH2DatabaseDataListener implements IPacketListener {

  private static final Type TYPE = new TypeToken<Map<String, Map<String, JsonDocument>>>() {
  }.getType();

  @Override
  public void handle(INetworkChannel channel, IPacket packet) {
    if (CloudNet.getInstance().getDatabaseProvider() instanceof H2DatabaseProvider && packet.getHeader()
      .contains("set_h2db")) {
      Map<String, Map<String, JsonDocument>> documents = packet.getHeader().get("documents", TYPE);

      H2DatabaseProvider databaseProvider = this.getH2DatabaseProvider();

      for (String name : databaseProvider.getDatabaseNames()) {
        if (!documents.containsKey(name)) {
          databaseProvider.deleteDatabase(name);
          continue;
        }

        H2Database database = databaseProvider.getDatabase(name);

        try {
          database.clear0();
        } catch (Exception exception) {
          exception.printStackTrace();
        }
      }

      for (Map.Entry<String, Map<String, JsonDocument>> db : documents.entrySet()) {
        H2Database database = databaseProvider.getDatabase(db.getKey());

        for (Map.Entry<String, JsonDocument> entry : documents.get(db.getKey()).entrySet()) {
          database.insertOrUpdate(entry.getKey(), entry.getValue());
        }
      }

      for (Map.Entry<String, Map<String, JsonDocument>> entry : documents.entrySet()) {
        entry.getValue().clear();
      }

      documents.clear();
    }
  }

  public H2DatabaseProvider getH2DatabaseProvider() {
    return (H2DatabaseProvider) CloudNet.getInstance().getDatabaseProvider();
  }
}
