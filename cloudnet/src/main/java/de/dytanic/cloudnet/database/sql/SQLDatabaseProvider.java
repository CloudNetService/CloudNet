package de.dytanic.cloudnet.database.sql;

import de.dytanic.cloudnet.common.concurrent.IThrowableCallback;
import de.dytanic.cloudnet.database.AbstractDatabaseProvider;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

public abstract class SQLDatabaseProvider extends AbstractDatabaseProvider {

    Collection<String> cachedDatabaseInstances;

    public abstract Connection getConnection() throws SQLException;

    public abstract int executeUpdate(String query, Object... objects);

    public abstract <T> T executeQuery(String query, IThrowableCallback<ResultSet, T> callback, Object... objects);

}
