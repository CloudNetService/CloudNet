package de.dytanic.cloudnet.ext.database.mysql;

import com.zaxxer.hikari.HikariDataSource;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.collection.NetorHashMap;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.concurrent.IThrowableCallback;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.AbstractDatabaseProvider;
import de.dytanic.cloudnet.database.IDatabase;
import de.dytanic.cloudnet.ext.database.mysql.util.MySQLConnectionEndpoint;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Getter
@RequiredArgsConstructor
public final class MySQLDatabaseProvider extends AbstractDatabaseProvider {

    private static final long NEW_CREATION_DELAY = 600000;

    /*= ---------------------------------------------------------------------- =*/

    protected final NetorHashMap<String, Long, MySQLDatabase> cachedDatabaseInstances = new NetorHashMap<>();

    protected final HikariDataSource hikariDataSource = new HikariDataSource();

    private final JsonDocument config;

    private List<MySQLConnectionEndpoint> addresses;

    @Override
    public boolean init() throws Exception {
        addresses = config.get("addresses", CloudNetMySQLDatabaseModule.TYPE);
        MySQLConnectionEndpoint endpoint = addresses.get(new Random().nextInt(addresses.size()));

        hikariDataSource.setJdbcUrl("jdbc:mysql://" + endpoint.getAddress().getHost() + ":" + endpoint.getAddress().getPort() + "/" + endpoint.getDatabase() +
                (endpoint.isUseSsl() ? "?useSSL=true&trustServerCertificate=true" : "")
        );

        //base configuration
        hikariDataSource.setUsername(config.getString("username"));
        hikariDataSource.setPassword(config.getString("password"));
        hikariDataSource.setDriverClassName("com.mysql.jdbc.Driver");

        hikariDataSource.setMaximumPoolSize(config.getInt("connectionPoolSize"));
        hikariDataSource.setConnectionTimeout(config.getInt("connectionTimeout"));
        hikariDataSource.setValidationTimeout(config.getInt("validationTimeout"));

        hikariDataSource.validate();
        return true;
    }

    @Override
    public IDatabase getDatabase(String name) {
        Validate.checkNotNull(name);

        removedOutdatedEntries();

        if (!cachedDatabaseInstances.contains(name))
            cachedDatabaseInstances.add(name, System.currentTimeMillis() + NEW_CREATION_DELAY, new MySQLDatabase(this, name));

        return cachedDatabaseInstances.getSecond(name);
    }

    @Override
    public boolean containsDatabase(String name) {
        Validate.checkNotNull(name);

        removedOutdatedEntries();

        for (String database : getDatabaseNames())
            if (database.equalsIgnoreCase(name)) return true;

        return false;
    }

    @Override
    public boolean deleteDatabase(String name) {
        Validate.checkNotNull(name);

        cachedDatabaseInstances.remove(name);

        if (containsDatabase(name))
            try (Connection connection = getConnection();
                 PreparedStatement preparedStatement = connection.prepareStatement("DROP TABLE " + name)) {
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
                        while (resultSet.next()) collection.add(resultSet.getString("table_name"));

                        return collection;
                    }
                }
        );
    }

    @Override
    public String getName() {
        return config.getString("database");
    }

    @Override
    public void close() throws Exception {
        hikariDataSource.close();
    }

    /*= ------------------------------------------------------------ =*/

    public int executeUpdate(String query, Object... objects) {
        Validate.checkNotNull(query);
        Validate.checkNotNull(objects);

        try (
                Connection connection = getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            int i = 1;
            for (Object object : objects)
                preparedStatement.setString(i++, object.toString());

            return preparedStatement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return -1;
    }

    public <T> T executeQuery(String query, IThrowableCallback<ResultSet, T> callback, Object... objects) {
        Validate.checkNotNull(query);
        Validate.checkNotNull(callback);
        Validate.checkNotNull(objects);

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            int i = 1;
            for (Object object : objects)
                preparedStatement.setString(i++, object.toString());

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
        for (Map.Entry<String, Pair<Long, MySQLDatabase>> entry : cachedDatabaseInstances.entrySet())
            if (entry.getValue().getFirst() < System.currentTimeMillis())
                cachedDatabaseInstances.remove(entry.getKey());
    }

    private Connection getConnection() throws SQLException {
        return hikariDataSource.getConnection();
    }
}