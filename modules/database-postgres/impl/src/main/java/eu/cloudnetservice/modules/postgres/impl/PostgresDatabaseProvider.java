/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.modules.postgres.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import eu.cloudnetservice.modules.postgres.config.PostgresConfiguration;
import eu.cloudnetservice.node.database.LocalDatabase;
import eu.cloudnetservice.node.impl.database.sql.SQLDatabaseProvider;
import io.vavr.CheckedFunction1;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public final class PostgresDatabaseProvider extends SQLDatabaseProvider {

  private static final String CONNECT_URL_FORMAT = "jdbc:postgresql://%s:%d/%s";

  private final PostgresConfiguration config;
  private volatile HikariDataSource hikariDataSource;

  public PostgresDatabaseProvider(
    @NonNull PostgresConfiguration config
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
    hikariConfig.setDriverClassName("org.postgresql.Driver");
    hikariConfig.setUsername(this.config.username());
    hikariConfig.setPassword(this.config.password());

    // reasonable defaults
    hikariConfig.setMinimumIdle(2);
    hikariConfig.setMaximumPoolSize(100);
    hikariConfig.setConnectionTimeout(10_000);
    hikariConfig.setValidationTimeout(10_000);

    this.hikariDataSource = new HikariDataSource(hikariConfig);
    return true;
  }

  @Override
  public @NonNull LocalDatabase database(@NonNull String name) {
    return this.databaseCache.get(name, _ -> new PostgresDatabase(this, name));
  }

  @Override
  public boolean deleteDatabase(@NonNull String name) {
    return this.executeUpdate("DROP TABLE IF EXISTS \"" + name + "\";") != -1;
  }

  @Override
  public @NonNull Collection<String> databaseNames() {
    try (var connection = this.hikariDataSource.getConnection();
      var meta = connection.getMetaData().getTables(null, null, null, TABLE_TYPE)) {
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
      for (var i = 0; i < objects.length; i++) {
        statement.setObject(i + 1, objects[i]);
      }
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
      for (var i = 0; i < objects.length; i++) {
        statement.setObject(i + 1, objects[i]);
      }
      try (var resultSet = statement.executeQuery()) {
        return callback.apply(resultSet);
      }
    } catch (Throwable throwable) {
      LOGGER.error("Exception while executing database query", throwable);
    }
    return def;
  }
}
