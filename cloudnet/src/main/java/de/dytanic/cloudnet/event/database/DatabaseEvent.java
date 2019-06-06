package de.dytanic.cloudnet.event.database;

import de.dytanic.cloudnet.database.IDatabase;
import de.dytanic.cloudnet.driver.event.events.DriverEvent;

abstract class DatabaseEvent extends DriverEvent {

    private final IDatabase database;

    public DatabaseEvent(IDatabase database) {
        this.database = database;
    }

    public IDatabase getDatabase() {
        return this.database;
    }
}