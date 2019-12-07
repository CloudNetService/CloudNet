package de.dytanic.cloudnet.event.database;

import de.dytanic.cloudnet.database.Database;
import de.dytanic.cloudnet.database.IDatabase;

public class DatabaseDeleteEntryEvent extends DatabaseEvent {

    private String key;

    @Deprecated
    public DatabaseDeleteEntryEvent(IDatabase database, String key) {
        super(database);

        this.key = key;
    }

    public DatabaseDeleteEntryEvent(Database database, String key) {
        this((IDatabase) database, key);
    }

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}