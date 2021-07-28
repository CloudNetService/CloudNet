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

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.LocalDatabase;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.event.database.DatabaseClearEntriesEvent;
import de.dytanic.cloudnet.event.database.DatabaseDeleteEntryEvent;
import de.dytanic.cloudnet.event.database.DatabaseInsertEntryEvent;
import de.dytanic.cloudnet.event.database.DatabaseUpdateEntryEvent;
import de.dytanic.cloudnet.network.packet.PacketServerLocalDatabase;

public final class PacketServerLocalDatabaseListener implements IPacketListener {

  @Override
  public void handle(INetworkChannel channel, IPacket packet) {
    if (CloudNet.getInstance().getDatabaseProvider().needsClusterSync()) {
      LocalDatabase database = (LocalDatabase) CloudNet.getInstance().getDatabaseProvider()
        .getDatabase(packet.getHeader().getString("name"));

      switch (packet.getHeader().get("operationType", PacketServerLocalDatabase.OperationType.class)) {
        case INSERT: {
          String key = packet.getHeader().getString("key");
          JsonDocument document = packet.getHeader().getDocument("document");

          CloudNetDriver.getInstance().getEventManager()
            .callEvent(new DatabaseInsertEntryEvent(database, key, document));
          database.insertWithoutHandlerCall(key, document);
        }
        break;
        case UPDATE: {
          String key = packet.getHeader().getString("key");
          JsonDocument document = packet.getHeader().getDocument("document");

          CloudNetDriver.getInstance().getEventManager()
            .callEvent(new DatabaseUpdateEntryEvent(database, key, document));
          database.updateWithoutHandlerCall(key, document);
        }
        break;
        case DELETE: {
          String key = packet.getHeader().getString("key");

          CloudNetDriver.getInstance().getEventManager().callEvent(new DatabaseDeleteEntryEvent(database, key));
          database.deleteWithoutHandlerCall(key);
        }
        break;
        case CLEAR: {
          CloudNetDriver.getInstance().getEventManager().callEvent(new DatabaseClearEntriesEvent(database));
          database.clearWithoutHandlerCall();
        }
        break;
        default:
          break;
      }
    }
  }
}
