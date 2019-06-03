package de.dytanic.cloudnet.event.database;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.IDatabase;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DatabaseInsertEntryEvent extends DatabaseEvent {

    private String key;

    private JsonDocument document;

    public DatabaseInsertEntryEvent(IDatabase database, String key, JsonDocument document) {
        super(database);

        this.key = key;
        this.document = document;
    }
}