package de.dytanic.cloudnet.database.sql;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.collection.NetorHashMap;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.concurrent.IThrowableCallback;
import de.dytanic.cloudnet.database.AbstractDatabaseProvider;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class SQLDatabaseProvider extends AbstractDatabaseProvider {

  protected final ExecutorService executorService;
  protected final NetorHashMap<String, Long, SQLDatabase> cachedDatabaseInstances = new NetorHashMap<>();
  private final boolean autoShutdownExecutorService;

  public SQLDatabaseProvider(ExecutorService executorService) {
    if (executorService != null) {
      this.executorService = executorService;
    } else {
      this.executorService = Executors.newCachedThreadPool();
    }
    this.autoShutdownExecutorService = executorService == null;
  }

  @Override
  public boolean containsDatabase(String name) {
    Preconditions.checkNotNull(name);

    this.removedOutdatedEntries();

    for (String database : this.getDatabaseNames()) {
      if (database.equalsIgnoreCase(name)) {
        return true;
      }
    }

    return false;
  }

  protected void removedOutdatedEntries() {
    for (Map.Entry<String, Pair<Long, SQLDatabase>> entry : this.cachedDatabaseInstances.entrySet()) {
      if (entry.getValue().getFirst() < System.currentTimeMillis()) {
        this.cachedDatabaseInstances.remove(entry.getKey());
      }
    }
  }

  @Override
  public void close() throws Exception {
    if (this.autoShutdownExecutorService) {
      this.executorService.shutdownNow();
    }
  }

  public abstract Connection getConnection() throws SQLException;

  public abstract int executeUpdate(String query, Object... objects);

  public abstract <T> T executeQuery(String query, IThrowableCallback<ResultSet, T> callback, Object... objects);

}
