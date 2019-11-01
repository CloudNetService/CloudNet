package de.dytanic.cloudnet.database.sql;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.concurrent.IThrowableCallback;
import de.dytanic.cloudnet.database.AbstractDatabaseProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

public abstract class SQLDatabaseProvider extends AbstractDatabaseProvider {

    Collection<String> cachedDatabaseInstances;

    public abstract Connection getConnection() throws SQLException;

    public int executeUpdate(String query, Object... objects) {
        Validate.checkNotNull(query);
        Validate.checkNotNull(objects);

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
        Validate.checkNotNull(query);
        Validate.checkNotNull(callback);
        Validate.checkNotNull(objects);

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
