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

package de.dytanic.cloudnet.database.h2;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.concurrent.IThrowableCallback;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.database.sql.SQLDatabaseProvider;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import org.h2.Driver;

public final class H2DatabaseProvider extends SQLDatabaseProvider {

  private static final long NEW_CREATION_DELAY = 600_000;

  static {
    Driver.load();
  }

  protected final Path h2dbFile;
  protected final boolean runsInCluster;
  protected Connection connection;

  public H2DatabaseProvider(String h2File, boolean runsInCluster) {
    this(h2File, runsInCluster, null);
  }

  public H2DatabaseProvider(String h2File, boolean runsInCluster, ExecutorService executorService) {
    super(executorService);
    this.h2dbFile = Paths.get(h2File);
    this.runsInCluster = runsInCluster;
  }

  @Override
  public boolean init() throws Exception {
    FileUtils.createDirectoryReported(this.h2dbFile.getParent());
    this.connection = DriverManager.getConnection("jdbc:h2:" + this.h2dbFile.toAbsolutePath());

    if (this.runsInCluster) {
      CloudNetDriver.getInstance().getLogger().warning("============================================");
      CloudNetDriver.getInstance().getLogger().warning(" ");

      CloudNetDriver.getInstance().getLogger().warning(LanguageManager.getMessage("cloudnet-cluster-h2-warning"));

      CloudNetDriver.getInstance().getLogger().warning(" ");
      CloudNetDriver.getInstance().getLogger().warning("============================================");
    }

    return this.connection != null;
  }

  @Override
  public H2Database getDatabase(String name) {
    Preconditions.checkNotNull(name);

    this.removedOutdatedEntries();

    if (!this.cachedDatabaseInstances.contains(name)) {
      this.cachedDatabaseInstances
        .add(name, System.currentTimeMillis() + NEW_CREATION_DELAY, new H2Database(this, name, super.executorService));
    }

    return (H2Database) this.cachedDatabaseInstances.getSecond(name);
  }

  @Override
  public boolean deleteDatabase(String name) {
    Preconditions.checkNotNull(name);

    if (!this.containsDatabase(name)) {
      return false;
    }

    this.cachedDatabaseInstances.remove(name);

    try (PreparedStatement preparedStatement = this.connection
      .prepareStatement("DROP TABLE IF EXISTS `" + name + "`")) {
      return preparedStatement.executeUpdate() != -1;
    } catch (SQLException exception) {
      exception.printStackTrace();
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
  public Connection getConnection() {
    return this.connection;
  }

  public int executeUpdate(String query, Object... objects) {
    Preconditions.checkNotNull(query);
    Preconditions.checkNotNull(objects);

    try (PreparedStatement preparedStatement = this.getConnection().prepareStatement(query)) {
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

    try (PreparedStatement preparedStatement = this.getConnection().prepareStatement(query)) {
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
