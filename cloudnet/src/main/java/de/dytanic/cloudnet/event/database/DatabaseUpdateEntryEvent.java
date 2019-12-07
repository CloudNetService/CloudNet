package de.dytanic.cloudnet.event.database;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.Database;
import de.dytanic.cloudnet.database.IDatabase;

public class DatabaseUpdateEntryEvent extends DatabaseEvent {

    private String key;

    private JsonDocument document;

    @Deprecated
    public DatabaseUpdateEntryEvent(IDatabase database, String key, JsonDocument document) {
        super(database);

        this.key = key;
        this.document = document;
    }

    public DatabaseUpdateEntryEvent(Database database, String key, JsonDocument document) {
        this((IDatabase) database, key, document);
    }

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public JsonDocument getDocument() {
        return this.document;
    }

    public void setDocument(JsonDocument document) {
        this.document = document;
    }
}