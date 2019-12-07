package de.dytanic.cloudnet.event.database;

import de.dytanic.cloudnet.database.Database;
import de.dytanic.cloudnet.database.IDatabase;
import de.dytanic.cloudnet.database.IDatabaseAdapter;
import de.dytanic.cloudnet.driver.event.events.DriverEvent;

abstract class DatabaseEvent extends DriverEvent {

    private final Database database;

    @Deprecated
    public DatabaseEvent(IDatabase database) {
        this.database = new IDatabaseAdapter(database);
    }

    public DatabaseEvent(Database database) {
        this.database = database;
    }

    public Database getDatabase() {
        return this.database;
    }
}