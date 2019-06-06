package de.dytanic.cloudnet.database;

import de.dytanic.cloudnet.common.INameable;

import java.util.Collection;

public abstract class AbstractDatabaseProvider implements INameable, AutoCloseable {

    protected IDatabaseHandler databaseHandler;

    public abstract boolean init() throws Exception;

    public abstract IDatabase getDatabase(String name);

    public abstract boolean containsDatabase(String name);

    public abstract boolean deleteDatabase(String name);

    public abstract Collection<String> getDatabaseNames();

    public IDatabaseHandler getDatabaseHandler() {
        return this.databaseHandler;
    }

    public void setDatabaseHandler(IDatabaseHandler databaseHandler) {
        this.databaseHandler = databaseHandler;
    }
}