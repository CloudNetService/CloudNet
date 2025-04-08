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

package eu.cloudnetservice.node.impl.database.h2;

import eu.cloudnetservice.node.database.LocalDatabase;
import eu.cloudnetservice.node.impl.database.sql.SQLDatabaseProvider;
import eu.cloudnetservice.utils.base.StringUtil;
import eu.cloudnetservice.utils.base.io.FileUtil;
import io.vavr.CheckedFunction1;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import lombok.NonNull;
import org.h2.Driver;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

@Deprecated(forRemoval = true)
public final class H2DatabaseProvider extends SQLDatabaseProvider {

  static {
    Driver.load();
  }

  private final Path h2dbFile;
  private Connection connection;

  public H2DatabaseProvider(@NonNull String h2File) {
    super(DEFAULT_REMOVAL_LISTENER);
    this.h2dbFile = Path.of(h2File);
  }

  @Override
  public boolean init() throws Exception {
    FileUtil.createDirectory(this.h2dbFile.getParent());
    this.connection = DriverManager.getConnection("jdbc:h2:" + this.h2dbFile.toAbsolutePath());

    return this.connection != null;
  }

  @Override
  public @NonNull LocalDatabase database(@NonNull String name) {
    return this.databaseCache.get(name, $ -> new H2Database(this, name));
  }

  @Override
  public boolean deleteDatabase(@NonNull String name) {
    return this.executeUpdate("DROP TABLE IF EXISTS `" + name + "`") != -1;
  }

  @Override
  public @NonNull Collection<String> databaseNames() {
    try (var meta = this.connection.getMetaData().getTables(null, null, null, TABLE_TYPE)) {
      // now we just need to extract the name from of the tables from the result set
      Collection<String> names = new ArrayList<>();
      while (meta.next()) {
        names.add(StringUtil.toLower(meta.getString("table_name")));
      }
      return names;
    } catch (SQLException exception) {
      LOGGER.error("Exception listing tables", exception);
      return Set.of();
    }
  }

  @Override
  public @NonNull String name() {
    return "h2";
  }

  @Override
  public void close() throws Exception {
    super.close();

    if (this.connection != null) {
      this.connection.close();
    }
  }

  @Override
  public @NonNull Connection connection() {
    return this.connection;
  }

  @Override
  public int executeUpdate(@NonNull String query, @NonNull Object... objects) {
    try (var preparedStatement = this.connection().prepareStatement(query)) {
      for (var i = 0; i < objects.length; i++) {
        preparedStatement.setString(i + 1, objects[i].toString());
      }

      return preparedStatement.executeUpdate();
    } catch (SQLException exception) {
      LOGGER.error("Exception while executing database update", exception);
      return -1;
    }
  }

  @Override
  public @UnknownNullability <T> T executeQuery(
    @NonNull String query,
    @NonNull CheckedFunction1<ResultSet, T> callback,
    @Nullable T def,
    @NonNull Object... objects
  ) {
    try (var preparedStatement = this.connection().prepareStatement(query)) {
      for (var i = 0; i < objects.length; i++) {
        preparedStatement.setString(i + 1, objects[i].toString());
      }

      try (var resultSet = preparedStatement.executeQuery()) {
        return callback.apply(resultSet);
      }
    } catch (Throwable throwable) {
      LOGGER.error("Exception while executing database query", throwable);
      return null;
    }
  }
}
