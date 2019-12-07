package de.dytanic.cloudnet.database;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;

@Deprecated
public class IDatabaseHandlerAdapter implements DatabaseHandler {

    private IDatabaseHandler original;

    public IDatabaseHandlerAdapter(IDatabaseHandler original) {
        this.original = original;
    }

    @Override
    public void handleInsert(IDatabase database, String key, JsonDocument document) {
        original.handleInsert(database, key, document);
    }

    @Override
    public void handleUpdate(IDatabase database, String key, JsonDocument document) {
        original.handleUpdate(database, key, document);
    }

    @Override
    public void handleDelete(IDatabase database, String key) {
        original.handleDelete(database, key);
    }

    @Override
    public void handleClear(IDatabase database) {
        original.handleClear(database);
    }

    @Override
    public void handleInsert(Database database, String key, JsonDocument document) {
        original.handleInsert(database, key, document);
    }

    @Override
    public void handleUpdate(Database database, String key, JsonDocument document) {
        original.handleUpdate(database, key, document);
    }

    @Override
    public void handleDelete(Database database, String key) {
        original.handleDelete(database, key);
    }

    @Override
    public void handleClear(Database database) {
        original.handleClear(database);
    }
}
