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

import de.dytanic.cloudnet.common.concurrent.ITask;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

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
