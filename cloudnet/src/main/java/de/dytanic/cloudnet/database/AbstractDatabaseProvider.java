package de.dytanic.cloudnet.database;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.database.DatabaseProvider;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractDatabaseProvider implements DatabaseProvider, INameable, AutoCloseable {

  protected IDatabaseHandler databaseHandler;

  public abstract boolean init() throws Exception;

  public IDatabaseHandler getDatabaseHandler() {
    return this.databaseHandler;
  }

  public void setDatabaseHandler(IDatabaseHandler databaseHandler) {
    this.databaseHandler = databaseHandler;
  }

  @Override
  public @NotNull ITask<Boolean> containsDatabaseAsync(String name) {
    return CloudNet.getInstance().scheduleTask(() -> this.containsDatabase(name));
  }

  @Override
  public @NotNull ITask<Boolean> deleteDatabaseAsync(String name) {
    return CloudNet.getInstance().scheduleTask(() -> this.deleteDatabase(name));
  }

  @Override
  public @NotNull ITask<Collection<String>> getDatabaseNamesAsync() {
    return CloudNet.getInstance().scheduleTask(this::getDatabaseNames);
  }
}
