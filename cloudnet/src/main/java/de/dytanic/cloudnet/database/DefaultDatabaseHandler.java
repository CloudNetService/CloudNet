package de.dytanic.cloudnet.database;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.h2.H2Database;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.event.database.DatabaseClearEntriesEvent;
import de.dytanic.cloudnet.event.database.DatabaseDeleteEntryEvent;
import de.dytanic.cloudnet.event.database.DatabaseInsertEntryEvent;
import de.dytanic.cloudnet.event.database.DatabaseUpdateEntryEvent;
import de.dytanic.cloudnet.network.packet.PacketServerH2Database;

public final class DefaultDatabaseHandler implements IDatabaseHandler {

  @Override
  public void handleInsert(IDatabase database, String key,
    JsonDocument document) {
    CloudNetDriver.getInstance().getEventManager()
      .callEvent(new DatabaseInsertEntryEvent(database, key, document));

    if (database instanceof H2Database) {
      CloudNet.getInstance().getClusterNodeServerProvider().sendPacket(
        new PacketServerH2Database(
          PacketServerH2Database.OperationType.INSERT, database.getName(),
          key, document)
      );
    }
  }

  @Override
  public void handleUpdate(IDatabase database, String key,
    JsonDocument document) {
    CloudNetDriver.getInstance().getEventManager()
      .callEvent(new DatabaseUpdateEntryEvent(database, key, document));

    if (database instanceof H2Database) {
      CloudNet.getInstance().getClusterNodeServerProvider().sendPacket(
        new PacketServerH2Database(
          PacketServerH2Database.OperationType.UPDATE, database.getName(),
          key, document)
      );
    }
  }

  @Override
  public void handleDelete(IDatabase database, String key) {
    CloudNetDriver.getInstance().getEventManager()
      .callEvent(new DatabaseDeleteEntryEvent(database, key));

    if (database instanceof H2Database) {
      CloudNet.getInstance().getClusterNodeServerProvider().sendPacket(
        new PacketServerH2Database(
          PacketServerH2Database.OperationType.DELETE, database.getName(),
          key, null)
      );
    }
  }

  @Override
  public void handleClear(IDatabase database) {
    CloudNetDriver.getInstance().getEventManager()
      .callEvent(new DatabaseClearEntriesEvent(database));

    if (database instanceof H2Database) {
      CloudNet.getInstance().getClusterNodeServerProvider().sendPacket(
        new PacketServerH2Database(PacketServerH2Database.OperationType.CLEAR,
          database.getName(), null, null)
      );
    }
  }
}