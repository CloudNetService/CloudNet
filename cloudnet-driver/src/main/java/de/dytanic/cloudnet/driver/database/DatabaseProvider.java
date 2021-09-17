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

package de.dytanic.cloudnet.driver.database;

import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

/**
 * This DatabaseProvider gives access to different {@link Database}
 */
public interface DatabaseProvider {

  /**
   * Returns the already existing database or creates a new one with the given name
   *
   * @param name the name of the database
   * @return the corresponding database
   */
  Database getDatabase(String name);

  /**
   * @param name the name of the database
   * @return whether a database with the given name exists
   */
  boolean containsDatabase(String name);

  /**
   * @param name the name of the database
   * @return true if the database was deleted successfully, false if not
   */
  boolean deleteDatabase(String name);

  /**
   * @return all present database names
   */
  Collection<String> getDatabaseNames();

  /**
   * @param name the name of the database
   * @return whether a database with the given name exists
   */
  @NotNull
  default ITask<Boolean> containsDatabaseAsync(String name) {
    return CompletableTask.supplyAsync(() -> this.containsDatabase(name));
  }

  /**
   * @param name the name of the database
   * @return true if the database was deleted successfully, false if not
   */
  @NotNull
  default ITask<Boolean> deleteDatabaseAsync(String name) {
    return CompletableTask.supplyAsync(() -> this.deleteDatabase(name));
  }

  /**
   * @return all present database names
   */
  @NotNull
  default ITask<Collection<String>> getDatabaseNamesAsync() {
    return CompletableTask.supplyAsync(this::getDatabaseNames);
  }

}
