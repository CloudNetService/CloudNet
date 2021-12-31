/*
 * Copyright 2019-2022 CloudNetService team & contributors
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
import de.dytanic.cloudnet.common.concurrent.Task;
import java.util.Collection;
import lombok.NonNull;

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
  @NonNull Database database(@NonNull String name);

  /**
   * @param name the name of the database
   * @return whether a database with the given name exists
   */
  boolean containsDatabase(@NonNull String name);

  /**
   * @param name the name of the database
   * @return true if the database was deleted successfully, false if not
   */
  boolean deleteDatabase(@NonNull String name);

  /**
   * @return all present database names
   */
  @NonNull Collection<String> databaseNames();

  /**
   * @param name the name of the database
   * @return whether a database with the given name exists
   */
  default @NonNull Task<Boolean> containsDatabaseAsync(@NonNull String name) {
    return CompletableTask.supply(() -> this.containsDatabase(name));
  }

  /**
   * @param name the name of the database
   * @return true if the database was deleted successfully, false if not
   */
  default @NonNull Task<Boolean> deleteDatabaseAsync(@NonNull String name) {
    return CompletableTask.supply(() -> this.deleteDatabase(name));
  }

  /**
   * @return all present database names
   */
  default @NonNull Task<Collection<String>> databaseNamesAsync() {
    return CompletableTask.supply(this::databaseNames);
  }
}
