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

package de.dytanic.cloudnet.database;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.h2.H2Database;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.database.Database;
import de.dytanic.cloudnet.event.database.DatabaseClearEntriesEvent;
import de.dytanic.cloudnet.event.database.DatabaseDeleteEntryEvent;
import de.dytanic.cloudnet.event.database.DatabaseInsertEntryEvent;
import de.dytanic.cloudnet.event.database.DatabaseUpdateEntryEvent;
import de.dytanic.cloudnet.network.packet.PacketServerH2Database;

public final class DefaultDatabaseHandler implements IDatabaseHandler {

  @Override
  public void handleInsert(Database database, String key, JsonDocument document) {
    CloudNetDriver.getInstance().getEventManager()
      .callEvent(new DatabaseInsertEntryEvent((IDatabase) database, key, document));

    if (database instanceof H2Database) {
      CloudNet.getInstance().getClusterNodeServerProvider().sendPacket(
        new PacketServerH2Database(PacketServerH2Database.OperationType.INSERT, database.getName(), key, document)
      );
    }
  }

  @Override
  public void handleUpdate(Database database, String key, JsonDocument document) {
    CloudNetDriver.getInstance().getEventManager()
      .callEvent(new DatabaseUpdateEntryEvent((IDatabase) database, key, document));

    if (database instanceof H2Database) {
      CloudNet.getInstance().getClusterNodeServerProvider().sendPacket(
        new PacketServerH2Database(PacketServerH2Database.OperationType.UPDATE, database.getName(), key, document)
      );
    }
  }

  @Override
  public void handleDelete(Database database, String key) {
    CloudNetDriver.getInstance().getEventManager().callEvent(new DatabaseDeleteEntryEvent((IDatabase) database, key));

    if (database instanceof H2Database) {
      CloudNet.getInstance().getClusterNodeServerProvider().sendPacket(
        new PacketServerH2Database(PacketServerH2Database.OperationType.DELETE, database.getName(), key, null)
      );
    }
  }

  @Override
  public void handleClear(Database database) {
    CloudNetDriver.getInstance().getEventManager().callEvent(new DatabaseClearEntriesEvent((IDatabase) database));

    if (database instanceof H2Database) {
      CloudNet.getInstance().getClusterNodeServerProvider().sendPacket(
        new PacketServerH2Database(PacketServerH2Database.OperationType.CLEAR, database.getName(), null, null)
      );
    }
  }
}
