/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.node.impl.database.sql;

import eu.cloudnetservice.node.impl.database.AbstractDatabase;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

@Deprecated
@ApiStatus.ScheduledForRemoval(inVersion = "4.1")
public abstract class SQLDatabase extends AbstractDatabase {

  protected static final String TABLE_COLUMN_KEY = "Name";
  protected static final String TABLE_COLUMN_VAL = "Document";

  protected final SQLDatabaseProvider databaseProvider;

  public SQLDatabase(@NonNull SQLDatabaseProvider provider, @NonNull String name) {
    super(name, provider);
    this.databaseProvider = provider;
  }
}
