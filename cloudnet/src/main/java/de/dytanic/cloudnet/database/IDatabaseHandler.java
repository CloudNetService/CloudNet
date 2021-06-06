package de.dytanic.cloudnet.database;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.database.Database;

public interface IDatabaseHandler {

  void handleInsert(Database database, String key, JsonDocument document);

  void handleUpdate(Database database, String key, JsonDocument document);

  void handleDelete(Database database, String key);

  void handleClear(Database database);

}
