package de.dytanic.cloudnet.database.h2;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.collection.NetorHashMap;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.concurrent.DefaultTaskScheduler;
import de.dytanic.cloudnet.common.concurrent.ITaskScheduler;
import de.dytanic.cloudnet.common.concurrent.IThrowableCallback;
import de.dytanic.cloudnet.database.AbstractDatabaseProvider;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import org.h2.Driver;

public final class H2DatabaseProvider extends AbstractDatabaseProvider {

  private static final long NEW_CREATION_DELAY = 600000;

  protected final NetorHashMap<String, Long, H2Database> cachedDatabaseInstances = new NetorHashMap<>();

  protected final ITaskScheduler taskScheduler;

  protected final boolean autoShutdownTaskScheduler;

  protected final File h2dbFile;

  protected Connection connection;

  static {
    Driver.load();
  }

  public H2DatabaseProvider(String h2File) {
    this(h2File, null);
  }

  public H2DatabaseProvider(String h2File, ITaskScheduler taskScheduler) {
    if (taskScheduler != null) {
      this.taskScheduler = taskScheduler;
      autoShutdownTaskScheduler = false;
    } else {
      this.taskScheduler = new DefaultTaskScheduler(1);
      autoShutdownTaskScheduler = true;
    }

    this.h2dbFile = new File(h2File);
  }

  @Override
  public boolean init() throws Exception {
    this.h2dbFile.getParentFile().mkdirs();
    this.connection = DriverManager
      .getConnection("jdbc:h2:" + h2dbFile.getAbsolutePath());

    return this.connection != null;
  }

  @Override
  public H2Database getDatabase(String name) {
    Validate.checkNotNull(name);

    removedOutdatedEntries();

    if (!cachedDatabaseInstances.contains(name)) {
      cachedDatabaseInstances
        .add(name, System.currentTimeMillis() + NEW_CREATION_DELAY,
          new H2Database(this, name));
    }

    return cachedDatabaseInstances.getSecond(name);
  }

  @Override
  public boolean containsDatabase(String name) {
    Validate.checkNotNull(name);

    removedOutdatedEntries();

    for (String database : getDatabaseNames()) {
      if (database.equalsIgnoreCase(name)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public boolean deleteDatabase(String name) {
    Validate.checkNotNull(name);

    cachedDatabaseInstances.remove(name);

    try (PreparedStatement preparedStatement = connection
      .prepareStatement("DROP TABLE " + name)) {
      return preparedStatement.executeUpdate() != -1;
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return false;
  }

  @Override
  public Collection<String> getDatabaseNames() {
    return executeQuery(
      "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES  where TABLE_SCHEMA='PUBLIC'",
      new IThrowableCallback<ResultSet, Collection<String>>() {
        @Override
        public Collection<String> call(ResultSet resultSet) throws Throwable {
          Collection<String> collection = Iterables.newArrayList();
          while (resultSet.next()) {
            collection.add(resultSet.getString("table_name"));
          }

          return collection;
        }
      }
    );
  }

  @Override
  public String getName() {
    return "h2";
  }

  @Override
  public void close() throws Exception {
    if (autoShutdownTaskScheduler) {
      taskScheduler.shutdown();
    }

    if (connection != null) {
      connection.close();
    }
  }

  /*= ------------------------------------------------------------ =*/

  public int executeUpdate(String query, Object... objects) {
    Validate.checkNotNull(query);
    Validate.checkNotNull(objects);

    try (PreparedStatement preparedStatement = connection
      .prepareStatement(query)) {
      int i = 1;
      for (Object object : objects) {
        preparedStatement.setString(i++, object.toString());
      }

      return preparedStatement.executeUpdate();

    } catch (SQLException e) {
      e.printStackTrace();
    }

    return -1;
  }

  public <T> T executeQuery(String query,
    IThrowableCallback<ResultSet, T> callback, Object... objects) {
    Validate.checkNotNull(query);
    Validate.checkNotNull(callback);
    Validate.checkNotNull(objects);

    try (PreparedStatement preparedStatement = connection
      .prepareStatement(query)) {
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

  /*= ------------------------------------------------------------ =*/

  private void removedOutdatedEntries() {
    for (Map.Entry<String, Pair<Long, H2Database>> entry : cachedDatabaseInstances
      .entrySet()) {
      if (entry.getValue().getFirst() < System.currentTimeMillis()) {
        cachedDatabaseInstances.remove(entry.getKey());
      }
    }
  }
}