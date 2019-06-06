package de.dytanic.cloudnet.event.database;

import de.dytanic.cloudnet.database.IDatabase;

public class DatabaseClearEntriesEvent extends DatabaseEvent {

    public DatabaseClearEntriesEvent(IDatabase database) {
        super(database);
    }
}