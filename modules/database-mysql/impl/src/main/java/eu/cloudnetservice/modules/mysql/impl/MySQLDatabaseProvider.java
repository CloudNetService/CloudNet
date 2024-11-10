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

package eu.cloudnetservice.modules.mysql.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import eu.cloudnetservice.modules.mysql.config.MySQLConfiguration;
import eu.cloudnetservice.node.database.LocalDatabase;
import eu.cloudnetservice.node.impl.database.sql.SQLDatabaseProvider;
import io.vavr.CheckedFunction1;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public final class MySQLDatabaseProvider extends SQLDatabaseProvider {

  private static final String CONNECT_URL_FORMAT = "jdbc:mysql://%s:%d/%s?serverTimezone=UTC";

  private final MySQLConfiguration config;
  private volatile HikariDataSource hikariDataSource;

  public MySQLDatabaseProvider(
    @NonNull MySQLConfiguration config,
    @Nullable ExecutorService executorService
  ) {
    super(DEFAULT_REMOVAL_LISTENER);
    this.config = config;
  }

  @Override
  public boolean init() {
    var hikariConfig = new HikariConfig();
    var endpoint = this.config.randomEndpoint();

    hikariConfig.setJdbcUrl(String.format(
      CONNECT_URL_FORMAT,
      endpoint.address().host(), endpoint.address().port(), endpoint.database()));
    hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
    hikariConfig.setUsername(this.config.username());
    hikariConfig.setPassword(this.config.password());

    hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
    hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
    hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
    hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
    hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
    hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
    hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
    hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
    hikariConfig.addDataSourceProperty("maintainTimeStats", "false");

    hikariConfig.setMinimumIdle(2);
    hikariConfig.setMaximumPoolSize(100);
    hikariConfig.setConnectionTimeout(10_000);
    hikariConfig.setValidationTimeout(10_000);

    this.hikariDataSource = new HikariDataSource(hikariConfig);
    return true;
  }

  @Override
  public @NonNull LocalDatabase database(@NonNull String name) {
    return this.databaseCache.get(name, _ -> new MySQLDatabase(this, name));
  }

  @Override
  public boolean deleteDatabase(@NonNull String name) {
    return this.executeUpdate(String.format("DROP TABLE IF EXISTS `%s`;", name)) != -1;
  }

  @Override
  public @NonNull Collection<String> databaseNames() {
    try (var connection = this.hikariDataSource.getConnection();
      var meta = connection.getMetaData().getTables(null, null, null, TABLE_TYPE)) {
      // now we just need to extract the name from of the tables from the result set
      Collection<String> names = new ArrayList<>();
      while (meta.next()) {
        names.add(meta.getString("table_name"));
      }
      return names;
    } catch (SQLException exception) {
      LOGGER.error("Exception listing tables", exception);
      return Set.of();
    }
  }

  @Override
  public @NonNull String name() {
    return this.config.databaseServiceName();
  }

  @Override
  public void close() throws Exception {
    super.close();
    this.hikariDataSource.close();
  }

  @Override
  public @NonNull Connection connection() {
    try {
      return this.hikariDataSource.getConnection();
    } catch (SQLException exception) {
      throw new IllegalStateException("Unable to retrieve connection from pool", exception);
    }
  }

  @Override
  public int executeUpdate(@NonNull String query, @NonNull Object... objects) {
    try (var con = this.connection(); var statement = con.prepareStatement(query)) {
      // write all parameters
      for (var i = 0; i < objects.length; i++) {
        statement.setObject(i + 1, objects[i]);
      }

      // execute the statement
      return statement.executeUpdate();
    } catch (SQLException exception) {
      LOGGER.error("Exception while executing database update", exception);
      return -1;
    }
  }

  @Override
  public <T> @UnknownNullability T executeQuery(
    @NonNull String query,
    @NonNull CheckedFunction1<ResultSet, T> callback,
    @Nullable T def,
    @NonNull Object... objects
  ) {
    try (var con = this.connection(); var statement = con.prepareStatement(query)) {
      // write all parameters
      for (var i = 0; i < objects.length; i++) {
        statement.setObject(i + 1, objects[i]);
      }

      // execute the statement, apply to the result handler
      try (var resultSet = statement.executeQuery()) {
        return callback.apply(resultSet);
      }
    } catch (Throwable throwable) {
      LOGGER.error("Exception while executing database query", throwable);
    }

    return def;
  }
}
