package de.dytanic.cloudnet.database;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;

@Deprecated
public interface IDatabaseHandler {

    @Deprecated
    void handleInsert(IDatabase database, String key, JsonDocument document);

    @Deprecated
    void handleUpdate(IDatabase database, String key, JsonDocument document);

    @Deprecated
    void handleDelete(IDatabase database, String key);

    @Deprecated
    void handleClear(IDatabase database);

    default void handleInsert(Database database, String key, JsonDocument document) {
        handleInsert((IDatabase) database, key, document);
    }

    default void handleUpdate(Database database, String key, JsonDocument document) {
        handleUpdate((IDatabase) database, key, document);
    }

    default void handleDelete(Database database, String key) {
        handleDelete((IDatabase) database, key);
    }

    default void handleClear(Database database) {
        handleClear((IDatabase) database);
    }

}