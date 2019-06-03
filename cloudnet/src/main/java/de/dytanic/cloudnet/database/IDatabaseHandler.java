package de.dytanic.cloudnet.database;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;

public interface IDatabaseHandler {

    void handleInsert(IDatabase database, String key, JsonDocument document);

    void handleUpdate(IDatabase database, String key, JsonDocument document);

    void handleDelete(IDatabase database, String key);

    void handleClear(IDatabase database);

}