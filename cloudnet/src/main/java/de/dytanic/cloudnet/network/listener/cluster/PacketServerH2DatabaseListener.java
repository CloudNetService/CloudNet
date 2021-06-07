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
import de.dytanic.cloudnet.database.h2.H2Database;
import de.dytanic.cloudnet.database.h2.H2DatabaseProvider;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.event.database.DatabaseClearEntriesEvent;
import de.dytanic.cloudnet.event.database.DatabaseDeleteEntryEvent;
import de.dytanic.cloudnet.event.database.DatabaseInsertEntryEvent;
import de.dytanic.cloudnet.event.database.DatabaseUpdateEntryEvent;
import de.dytanic.cloudnet.network.packet.PacketServerH2Database;

public final class PacketServerH2DatabaseListener implements IPacketListener {

  @Override
  public void handle(INetworkChannel channel, IPacket packet) {
    if (packet.getHeader().contains("operationType") && packet.getHeader().contains("name")) {
      if (CloudNet.getInstance().getDatabaseProvider() instanceof H2DatabaseProvider) {
        H2Database database = (H2Database) CloudNet.getInstance().getDatabaseProvider()
          .getDatabase(packet.getHeader().getString("name"));

        switch (packet.getHeader().get("operationType", PacketServerH2Database.OperationType.class)) {
          case INSERT:
            if (packet.getHeader().contains("key") && packet.getHeader().contains("document")) {
              CloudNetDriver.getInstance().getEventManager().callEvent(
                new DatabaseInsertEntryEvent(database, packet.getHeader().getString("key"),
                  packet.getHeader().getDocument("document"))
              );
              database.insertOrUpdate(packet.getHeader().getString("key"), packet.getHeader().getDocument("document"));
            }
            break;
          case UPDATE:
            if (packet.getHeader().contains("key") && packet.getHeader().contains("document")) {
              CloudNetDriver.getInstance().getEventManager().callEvent(
                new DatabaseUpdateEntryEvent(database, packet.getHeader().getString("key"),
                  packet.getHeader().getDocument("document"))
              );
              database.insertOrUpdate(packet.getHeader().getString("key"), packet.getHeader().getDocument("document"));
            }
            break;
          case DELETE:
            if (packet.getHeader().contains("key")) {
              CloudNetDriver.getInstance().getEventManager().callEvent(
                new DatabaseDeleteEntryEvent(database, packet.getHeader().getString("key"))
              );
              database.delete0(packet.getHeader().getString("key"));
            }
            break;
          case CLEAR:
            CloudNetDriver.getInstance().getEventManager().callEvent(new DatabaseClearEntriesEvent(database));
            database.clear0();
            break;
          default:
            break;
        }
      }
    }
  }
}
