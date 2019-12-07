package de.dytanic.cloudnet.database;

import de.dytanic.cloudnet.common.Nameable;
import jline.internal.Nullable;

import java.util.Collection;

public abstract class AbstractDatabaseProvider implements Nameable, AutoCloseable {

    protected DatabaseHandler databaseHandler;

    public abstract boolean init() throws Exception;

    public abstract Database getDatabase(String name);

    public abstract boolean containsDatabase(String name);

    public abstract boolean deleteDatabase(String name);

    public abstract Collection<String> getDatabaseNames();

    public DatabaseHandler getDatabaseHandler() {
        return this.databaseHandler;
    }

    @Deprecated
    public void setDatabaseHandler(IDatabaseHandler databaseHandler) {
        this.databaseHandler = new IDatabaseHandlerAdapter(databaseHandler);
    }

    public void setDatabaseHandler(DatabaseHandler databaseHandler) {
        this.databaseHandler = databaseHandler;
    }
}