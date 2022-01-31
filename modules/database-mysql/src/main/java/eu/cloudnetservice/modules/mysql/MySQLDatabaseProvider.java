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

package eu.cloudnetservice.modules.mysql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import eu.cloudnetservice.cloudnet.common.function.ThrowableFunction;
import eu.cloudnetservice.cloudnet.node.database.LocalDatabase;
import eu.cloudnetservice.cloudnet.node.database.sql.SQLDatabaseProvider;
import eu.cloudnetservice.modules.mysql.config.MySQLConfiguration;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public final class MySQLDatabaseProvider extends SQLDatabaseProvider {

  private static final String CONNECT_URL_FORMAT = "jdbc:mysql://%s:%d/%s?serverTimezone=UTC&useSSL=%b&trustServerCertificate=%b";

  private final MySQLConfiguration config;
  private volatile HikariDataSource hikariDataSource;

  public MySQLDatabaseProvider(@NonNull MySQLConfiguration config, @Nullable ExecutorService executorService) {
    super(executorService);
    this.config = config;
  }

  @Override
  public boolean init() {
    var hikariConfig = new HikariConfig();
    var endpoint = this.config.randomEndpoint();

    hikariConfig.setJdbcUrl(String.format(
      CONNECT_URL_FORMAT,
      endpoint.address().host(), endpoint.address().port(),
      endpoint.database(), endpoint.useSsl(), endpoint.useSsl()
    ));
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
    return this.databaseCache.get(
      name,
      $ -> new MySQLDatabase(this, name, super.executorService));
  }

  @Override
  public boolean deleteDatabase(@NonNull String name) {
    return this.executeUpdate(String.format("DROP TABLE IF EXISTS `%s`;", name)) != -1;
  }

  @Override
  public @NonNull Collection<String> databaseNames() {
    return this.executeQuery(
      "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='PUBLIC'",
      resultSet -> {
        Collection<String> collection = new ArrayList<>();
        while (resultSet.next()) {
          collection.add(resultSet.getString("table_name"));
        }

        return collection;
      }, Set.of());
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
      for (int i = 0; i < objects.length; i++) {
        statement.setString(i + 1, Objects.toString(objects[i]));
      }

      // execute the statement
      return statement.executeUpdate();
    } catch (SQLException exception) {
      LOGGER.severe("Exception while executing database update", exception);
      return -1;
    }
  }

  @Override
  public <T> @UnknownNullability T executeQuery(
    @NonNull String query,
    @NonNull ThrowableFunction<ResultSet, T, SQLException> callback,
    @Nullable T def,
    @NonNull Object... objects
  ) {
    try (var con = this.connection(); var statement = con.prepareStatement(query)) {
      // write all parameters
      for (int i = 0; i < objects.length; i++) {
        statement.setString(i + 1, Objects.toString(objects[i]));
      }

      // execute the statement, apply to the result handler
      try (var resultSet = statement.executeQuery()) {
        return callback.apply(resultSet);
      }
    } catch (Throwable throwable) {
      LOGGER.severe("Exception while executing database query", throwable);
    }

    return def;
  }
}
