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

package eu.cloudnetservice.cloudnet.node.database.h2;

import eu.cloudnetservice.cloudnet.common.function.ThrowableFunction;
import eu.cloudnetservice.cloudnet.common.io.FileUtils;
import eu.cloudnetservice.cloudnet.node.database.sql.SQLDatabaseProvider;
import eu.cloudnetservice.cloudnet.node.database.util.LocalDatabaseUtils;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import lombok.NonNull;
import org.h2.Driver;
import org.jetbrains.annotations.Nullable;

public final class H2DatabaseProvider extends SQLDatabaseProvider {

  private static final long NEW_CREATION_DELAY = 600_000;

  static {
    Driver.load();
  }

  private final Path h2dbFile;
  private final boolean runsInCluster;
  private Connection connection;

  public H2DatabaseProvider(@NonNull String h2File, boolean runsInCluster) {
    this(h2File, runsInCluster, null);
  }

  public H2DatabaseProvider(@NonNull String h2File, boolean runsInCluster, @Nullable ExecutorService executorService) {
    super(executorService);
    this.h2dbFile = Path.of(h2File);
    this.runsInCluster = runsInCluster;
  }

  @Override
  public boolean init() throws Exception {
    LocalDatabaseUtils.bigWarningThatEveryoneCanSee(
      "! Using H2 is deprecated ! Consider migrating to xodus. H2 support will be dropped in a future release.");

    FileUtils.createDirectory(this.h2dbFile.getParent());
    this.connection = DriverManager.getConnection("jdbc:h2:" + this.h2dbFile.toAbsolutePath());

    return this.connection != null;
  }

  @Override
  public @NonNull H2Database database(@NonNull String name) {
    this.removedOutdatedEntries();
    return (H2Database) this.cachedDatabaseInstances.computeIfAbsent(name,
      $ -> new H2Database(this, name, NEW_CREATION_DELAY, super.executorService));
  }

  @Override
  public boolean deleteDatabase(@NonNull String name) {
    if (!this.containsDatabase(name)) {
      return false;
    }

    this.cachedDatabaseInstances.remove(name);
    return this.executeUpdate("DROP TABLE IF EXISTS `" + name + "`") != -1;
  }

  @Override
  public @NonNull Collection<String> databaseNames() {
    var tableNames = this.executeQuery(
      "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES where TABLE_SCHEMA='PUBLIC'",
      resultSet -> {
        Collection<String> collection = new ArrayList<>();
        while (resultSet.next()) {
          collection.add(resultSet.getString("table_name"));
        }

        return collection;
      });
    return tableNames == null ? Collections.emptyList() : tableNames;
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

  public int executeUpdate(@NonNull String query, @NonNull Object... objects) {
    try (var preparedStatement = this.connection().prepareStatement(query)) {
      for (var i = 0; i < objects.length; i++) {
        preparedStatement.setString(i + 1, objects[i].toString());
      }

      return preparedStatement.executeUpdate();
    } catch (SQLException exception) {
      LOGGER.severe("Exception while executing database update", exception);
      return -1;
    }
  }

  public @Nullable <T> T executeQuery(
    @NonNull String query,
    @NonNull ThrowableFunction<ResultSet, T, SQLException> callback,
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
      LOGGER.severe("Exception while executing database query", throwable);
      return null;
    }
  }
}
