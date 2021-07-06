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

package de.dytanic.cloudnet.ext.database.mysql;

import com.google.common.base.Preconditions;
import com.zaxxer.hikari.HikariDataSource;
import de.dytanic.cloudnet.common.concurrent.IThrowableCallback;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.IDatabase;
import de.dytanic.cloudnet.database.sql.SQLDatabaseProvider;
import de.dytanic.cloudnet.ext.database.mysql.util.MySQLConnectionEndpoint;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;

public final class MySQLDatabaseProvider extends SQLDatabaseProvider {

  private static final long NEW_CREATION_DELAY = 600000;


  protected final HikariDataSource hikariDataSource = new HikariDataSource();

  private final JsonDocument config;

  private List<MySQLConnectionEndpoint> addresses;

  public MySQLDatabaseProvider(JsonDocument config, ExecutorService executorService) {
    super(executorService);
    this.config = config;
  }

  @Override
  public boolean init() {
    this.addresses = this.config.get("addresses", CloudNetMySQLDatabaseModule.TYPE);
    MySQLConnectionEndpoint endpoint = this.addresses.get(new Random().nextInt(this.addresses.size()));

    this.hikariDataSource.setJdbcUrl(
      "jdbc:mysql://" + endpoint.getAddress().getHost() + ":" + endpoint.getAddress().getPort() + "/" + endpoint
        .getDatabase() +
        String.format("?useSSL=%b&trustServerCertificate=%b", endpoint.isUseSsl(), endpoint.isUseSsl())
    );

    //base configuration
    this.hikariDataSource.setUsername(this.config.getString("username"));
    this.hikariDataSource.setPassword(this.config.getString("password"));
    this.hikariDataSource.setDriverClassName("com.mysql.jdbc.Driver");

    int maxPoolSize = this.config.getInt("connectionMaxPoolSize");

    this.hikariDataSource.setMaximumPoolSize(maxPoolSize);
    this.hikariDataSource.setMinimumIdle(Math.min(maxPoolSize, this.config.getInt("connectionMinPoolSize")));
    this.hikariDataSource.setConnectionTimeout(this.config.getInt("connectionTimeout"));
    this.hikariDataSource.setValidationTimeout(this.config.getInt("validationTimeout"));

    this.hikariDataSource.validate();
    return true;
  }

  @Override
  public IDatabase getDatabase(String name) {
    Preconditions.checkNotNull(name);

    this.removedOutdatedEntries();

    if (!this.cachedDatabaseInstances.contains(name)) {
      this.cachedDatabaseInstances.add(name, System.currentTimeMillis() + NEW_CREATION_DELAY,
        new MySQLDatabase(this, name, super.executorService));
    }

    return this.cachedDatabaseInstances.getSecond(name);
  }

  @Override
  public boolean deleteDatabase(String name) {
    Preconditions.checkNotNull(name);

    this.cachedDatabaseInstances.remove(name);

    if (this.containsDatabase(name)) {
      try (Connection connection = this.getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement("DROP TABLE " + name)) {
        return preparedStatement.executeUpdate() != -1;
      } catch (SQLException exception) {
        exception.printStackTrace();
      }
    }

    return false;
  }

  @Override
  public Collection<String> getDatabaseNames() {
    return this.executeQuery(
      "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES  where TABLE_SCHEMA='PUBLIC'",
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
  public String getName() {
    return this.config.getString("database");
  }

  @Override
  public void close() throws Exception {
    super.close();

    this.hikariDataSource.close();
  }

  public Connection getConnection() throws SQLException {
    return this.hikariDataSource.getConnection();
  }

  public HikariDataSource getHikariDataSource() {
    return this.hikariDataSource;
  }

  public JsonDocument getConfig() {
    return this.config;
  }

  public List<MySQLConnectionEndpoint> getAddresses() {
    return this.addresses;
  }

  public int executeUpdate(String query, Object... objects) {
    Preconditions.checkNotNull(query);
    Preconditions.checkNotNull(objects);

    try (Connection connection = this.getConnection();
      PreparedStatement preparedStatement = connection.prepareStatement(query)) {
      int i = 1;
      for (Object object : objects) {
        preparedStatement.setString(i++, object.toString());
      }

      return preparedStatement.executeUpdate();

    } catch (SQLException exception) {
      exception.printStackTrace();
    }

    return -1;
  }

  public <T> T executeQuery(String query, IThrowableCallback<ResultSet, T> callback, Object... objects) {
    Preconditions.checkNotNull(query);
    Preconditions.checkNotNull(callback);
    Preconditions.checkNotNull(objects);

    try (Connection connection = this.getConnection();
      PreparedStatement preparedStatement = connection.prepareStatement(query)) {
      int i = 1;
      for (Object object : objects) {
        preparedStatement.setString(i++, object.toString());
      }

      try (ResultSet resultSet = preparedStatement.executeQuery()) {
        return callback.call(resultSet);
      }

    } catch (Throwable e) {
      e.printStackTrace();
    }

    return null;
  }

}
