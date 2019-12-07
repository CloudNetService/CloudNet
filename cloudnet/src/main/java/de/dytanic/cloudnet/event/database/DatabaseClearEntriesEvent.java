package de.dytanic.cloudnet.event.database;

import de.dytanic.cloudnet.database.Database;
import de.dytanic.cloudnet.database.IDatabase;

public class DatabaseClearEntriesEvent extends DatabaseEvent {

    @Deprecated
    public DatabaseClearEntriesEvent(IDatabase database) {
        super(database);
    }

    public DatabaseClearEntriesEvent(Database database) {
        super((IDatabase) database);
    }
}