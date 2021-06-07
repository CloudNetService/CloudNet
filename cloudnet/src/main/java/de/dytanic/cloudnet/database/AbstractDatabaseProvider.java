/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
