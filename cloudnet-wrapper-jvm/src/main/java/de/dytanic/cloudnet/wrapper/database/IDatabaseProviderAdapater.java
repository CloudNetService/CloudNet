package de.dytanic.cloudnet.wrapper.database;

import de.dytanic.cloudnet.common.concurrent.ITask;

import java.util.Collection;

public class IDatabaseProviderAdapater implements DatabaseProvider {

    private IDatabaseProvider original;

    public IDatabaseProviderAdapater(IDatabaseProvider original) {
        this.original = original;
    }

    @Override
    public IDatabase getDatabase(String name) {
        return original.getDatabase(name);
    }

    @Override
    public boolean containsDatabase(String name) {
        return original.containsDatabase(name);
    }

    @Override
    public boolean deleteDatabase(String name) {
        return original.deleteDatabase(name);
    }

    @Override
    public Collection<String> getDatabaseNames() {
        return original.getDatabaseNames();
    }

    @Override
    public ITask<Boolean> containsDatabaseAsync(String name) {
        return original.containsDatabaseAsync(name);
    }

    @Override
    public ITask<Boolean> deleteDatabaseAsync(String name) {
        return original.deleteDatabaseAsync(name);
    }

    @Override
    public ITask<Collection<String>> getDatabaseNamesAsync() {
        return original.getDatabaseNamesAsync();
    }
}
