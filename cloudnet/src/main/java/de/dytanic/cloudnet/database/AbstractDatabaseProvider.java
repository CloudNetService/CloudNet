package de.dytanic.cloudnet.database;

import de.dytanic.cloudnet.common.INameable;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;

public abstract class AbstractDatabaseProvider implements INameable, AutoCloseable {

    @Getter
    @Setter
    protected IDatabaseHandler databaseHandler;

    public abstract boolean init() throws Exception;

    public abstract IDatabase getDatabase(String name);

    public abstract boolean containsDatabase(String name);

    public abstract boolean deleteDatabase(String name);

    public abstract Collection<String> getDatabaseNames();

}