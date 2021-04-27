package de.dytanic.cloudnet.driver.database;

import de.dytanic.cloudnet.common.concurrent.ITask;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface DatabaseProvider {

    Database getDatabase(String name);

    boolean containsDatabase(String name);

    boolean deleteDatabase(String name);

    Collection<String> getDatabaseNames();


    @NotNull
    ITask<Boolean> containsDatabaseAsync(String name);

    @NotNull
    ITask<Boolean> deleteDatabaseAsync(String name);

    @NotNull
    ITask<Collection<String>> getDatabaseNamesAsync();

}
