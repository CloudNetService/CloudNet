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

package de.dytanic.cloudnet.ext.database.mysql;

import com.google.common.base.Preconditions;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.function.ThrowableFunction;
import de.dytanic.cloudnet.database.LocalDatabase;
import de.dytanic.cloudnet.database.sql.SQLDatabaseProvider;
import de.dytanic.cloudnet.ext.database.mysql.util.MySQLConnectionEndpoint;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import lombok.NonNull;

public final class MySQLDatabaseProvider extends SQLDatabaseProvider {

  private static final long NEW_CREATION_DELAY = 600_000;
  private static final String CONNECT_URL_FORMAT = "jdbc:mysql://%s:%d/%s?serverTimezone=UTC&useSSL=%b&trustServerCertificate=%b";

  private final JsonDocument config;

  private HikariDataSource hikariDataSource;
  private List<MySQLConnectionEndpoint> addresses;

  public MySQLDatabaseProvider(JsonDocument config, ExecutorService executorService) {
    super(executorService);
    this.config = config;
  }

  @Override
  public boolean init() {
    this.addresses = this.config.get("addresses", CloudNetMySQLDatabaseModule.TYPE);
    var endpoint = this.addresses.get(new Random().nextInt(this.addresses.size()));

    var hikariConfig = new HikariConfig();

    hikariConfig.setJdbcUrl(String.format(
      CONNECT_URL_FORMAT,
      endpoint.address().host(), endpoint.address().port(),
      endpoint.database(), endpoint.useSsl(), endpoint.useSsl()
    ));
    hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
    hikariConfig.setUsername(this.config.getString("username"));
    hikariConfig.setPassword(this.config.getString("password"));

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

    var maxPoolSize = this.config.getInt("connectionMaxPoolSize");

    hikariConfig.setMaximumPoolSize(maxPoolSize);
    hikariConfig.setMinimumIdle(Math.min(maxPoolSize, this.config.getInt("connectionMinPoolSize")));
    hikariConfig.setConnectionTimeout(this.config.getInt("connectionTimeout"));
    hikariConfig.setValidationTimeout(this.config.getInt("validationTimeout"));

    this.hikariDataSource = new HikariDataSource(hikariConfig);
    return true;
  }

  @Override
  public @NonNull LocalDatabase database(@NonNull String name) {
    Preconditions.checkNotNull(name);

    this.removedOutdatedEntries();
    return this.cachedDatabaseInstances.computeIfAbsent(name,
      $ -> new MySQLDatabase(this, name, NEW_CREATION_DELAY, super.executorService));
  }

  @Override
  public boolean deleteDatabase(@NonNull String name) {
    Preconditions.checkNotNull(name);

    this.cachedDatabaseInstances.remove(name);
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
      }
    );
  }

  @Override
  public @NonNull String name() {
    return this.config.getString("database");
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
      LOGGER.severe("Exception while opening connection", exception);
    }

    return null;
  }

  public HikariDataSource hikariDataSource() {
    return this.hikariDataSource;
  }

  public JsonDocument config() {
    return this.config;
  }

  public List<MySQLConnectionEndpoint> addresses() {
    return this.addresses;
  }

  @Override
  public int executeUpdate(@NonNull String query, Object... objects) {
    Preconditions.checkNotNull(query);
    Preconditions.checkNotNull(objects);

    try (var connection = this.connection();
      var preparedStatement = connection.prepareStatement(query)) {
      var i = 1;
      for (var object : objects) {
        preparedStatement.setString(i++, object.toString());
      }

      return preparedStatement.executeUpdate();

    } catch (SQLException exception) {
      LOGGER.severe("Exception while executing database update", exception);
    }

    return -1;
  }

  @Override
  public <T> T executeQuery(@NonNull String query, @NonNull ThrowableFunction<ResultSet, T, SQLException> callback,
    Object... objects) {
    Preconditions.checkNotNull(query);
    Preconditions.checkNotNull(callback);
    Preconditions.checkNotNull(objects);

    try (var connection = this.connection();
      var preparedStatement = connection.prepareStatement(query)) {
      var i = 1;
      for (var object : objects) {
        preparedStatement.setString(i++, object.toString());
      }

      try (var resultSet = preparedStatement.executeQuery()) {
        return callback.apply(resultSet);
      }
    } catch (Throwable throwable) {
      LOGGER.severe("Exception while executing database query", throwable);
    }

    return null;
  }

}
