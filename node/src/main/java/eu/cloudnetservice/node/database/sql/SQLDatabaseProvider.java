/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.node.database.sql;

import com.github.benmanes.caffeine.cache.RemovalListener;
import eu.cloudnetservice.common.function.ThrowableFunction;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.node.database.LocalDatabase;
import eu.cloudnetservice.node.database.NodeDatabaseProvider;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

@Deprecated
@ApiStatus.ScheduledForRemoval(inVersion = "4.1")
public abstract class SQLDatabaseProvider extends NodeDatabaseProvider {

  protected static final String[] TABLE_TYPE = new String[]{"TABLE"};
  protected static final Logger LOGGER = LogManager.logger(SQLDatabaseProvider.class);

  protected SQLDatabaseProvider(@NonNull RemovalListener<String, LocalDatabase> removalListener) {
    super(removalListener);
  }

  @Override
  public boolean containsDatabase(@NonNull String name) {
    for (var database : this.databaseNames()) {
      if (database.equalsIgnoreCase(name)) {
        return true;
      }
    }

    return false;
  }

  public abstract @NonNull Connection connection();

  public abstract int executeUpdate(@NonNull String query, @NonNull Object... objects);

  public abstract <T> @UnknownNullability T executeQuery(
    @NonNull String query,
    @NonNull ThrowableFunction<ResultSet, T, SQLException> callback,
    @Nullable T def,
    @NonNull Object... objects);
}
