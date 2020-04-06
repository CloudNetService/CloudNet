package de.dytanic.cloudnet.database.sql;

import de.dytanic.cloudnet.common.concurrent.IThrowableCallback;
import de.dytanic.cloudnet.database.AbstractDatabaseProvider;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class SQLDatabaseProvider extends AbstractDatabaseProvider {

    protected final ExecutorService executorService;
    private final boolean autoShutdownExecutorService;

    Collection<String> cachedDatabaseInstances;

    public SQLDatabaseProvider(ExecutorService executorService) {
        if (executorService != null) {
            this.executorService = executorService;
        } else {
            this.executorService = Executors.newCachedThreadPool();
        }
        this.autoShutdownExecutorService = executorService == null;
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
