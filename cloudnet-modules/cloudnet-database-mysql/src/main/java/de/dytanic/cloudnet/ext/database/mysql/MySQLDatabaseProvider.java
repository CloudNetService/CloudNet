package de.dytanic.cloudnet.ext.database.mysql;

import com.zaxxer.hikari.HikariDataSource;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.collection.NetorHashMap;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.concurrent.IThrowableCallback;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.IDatabase;
import de.dytanic.cloudnet.database.sql.SQLDatabaseProvider;
import de.dytanic.cloudnet.ext.database.mysql.util.MySQLConnectionEndpoint;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class MySQLDatabaseProvider extends SQLDatabaseProvider {

    private static final long NEW_CREATION_DELAY = 600000;


    protected final NetorHashMap<String, Long, MySQLDatabase> cachedDatabaseInstances = new NetorHashMap<>();

    protected final HikariDataSource hikariDataSource = new HikariDataSource();

    private final JsonDocument config;

    private List<MySQLConnectionEndpoint> addresses;

    public MySQLDatabaseProvider(JsonDocument config) {
        this.config = config;
    }

    @Override
    public boolean init() {
        this.addresses = this.config.get("addresses", CloudNetMySQLDatabaseModule.TYPE);
        MySQLConnectionEndpoint endpoint = this.addresses.get(new Random().nextInt(this.addresses.size()));

        this.hikariDataSource.setJdbcUrl("jdbc:mysql://" + endpoint.getAddress().getHost() + ":" + endpoint.getAddress().getPort() + "/" + endpoint.getDatabase() +
                (endpoint.isUseSsl() ? "?useSSL=true&trustServerCertificate=true" : "")
        );

        //base configuration
        this.hikariDataSource.setUsername(this.config.getString("username"));
        this.hikariDataSource.setPassword(this.config.getString("password"));
        this.hikariDataSource.setDriverClassName("com.mysql.jdbc.Driver");

        this.hikariDataSource.setMaximumPoolSize(this.config.getInt("connectionPoolSize"));
        this.hikariDataSource.setConnectionTimeout(this.config.getInt("connectionTimeout"));
        this.hikariDataSource.setValidationTimeout(this.config.getInt("validationTimeout"));

        this.hikariDataSource.validate();
        return true;
    }

    @Override
    public IDatabase getDatabase(String name) {
        Validate.checkNotNull(name);

        this.removedOutdatedEntries();

        if (!this.cachedDatabaseInstances.contains(name)) {
            this.cachedDatabaseInstances.add(name, System.currentTimeMillis() + NEW_CREATION_DELAY, new MySQLDatabase(this, name));
        }

        return cachedDatabaseInstances.getSecond(name);
    }

    @Override
    public boolean containsDatabase(String name) {
        Validate.checkNotNull(name);

        this.removedOutdatedEntries();

        for (String database : this.getDatabaseNames()) {
            if (database.equalsIgnoreCase(name)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean deleteDatabase(String name) {
        Validate.checkNotNull(name);

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
                    Collection<String> collection = Iterables.newArrayList();
                    while (resultSet.next()) {
                        collection.add(resultSet.getString("table_name"));
                    }

                    return collection;
                }
        );
    }

    @Override
    public String getName() {
        return config.getString("database");
    }

    @Override
    public void close() {
        this.hikariDataSource.close();
    }


    private void removedOutdatedEntries() {
        for (Map.Entry<String, Pair<Long, MySQLDatabase>> entry : this.cachedDatabaseInstances.entrySet()) {
            if (entry.getValue().getFirst() < System.currentTimeMillis()) {
                this.cachedDatabaseInstances.remove(entry.getKey());
            }
        }
    }

    public Connection getConnection() throws SQLException {
        return this.hikariDataSource.getConnection();
    }

    public NetorHashMap<String, Long, MySQLDatabase> getCachedDatabaseInstances() {
        return this.cachedDatabaseInstances;
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
        Validate.checkNotNull(query);
        Validate.checkNotNull(objects);

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
        Validate.checkNotNull(query);
        Validate.checkNotNull(callback);
        Validate.checkNotNull(objects);

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