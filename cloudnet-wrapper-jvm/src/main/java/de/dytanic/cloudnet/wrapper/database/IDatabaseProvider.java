package de.dytanic.cloudnet.wrapper.database;

import de.dytanic.cloudnet.common.concurrent.ITask;

import java.util.Collection;

public interface IDatabaseProvider {

    IDatabase getDatabase(String name);

    boolean containsDatabase(String name);

    boolean deleteDatabase(String name);

    Collection<String> getDatabaseNames();


    ITask<Boolean> containsDatabaseAsync(String name);

    ITask<Boolean> deleteDatabaseAsync(String name);

    ITask<Collection<String>> getDatabaseNamesAsync();

}
