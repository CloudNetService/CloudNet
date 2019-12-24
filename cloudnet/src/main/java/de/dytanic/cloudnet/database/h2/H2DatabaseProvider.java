package de.dytanic.cloudnet.database.h2;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.collection.NetorHashMap;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.concurrent.DefaultTaskScheduler;
import de.dytanic.cloudnet.common.concurrent.ITaskScheduler;
import de.dytanic.cloudnet.common.concurrent.IThrowableCallback;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.database.sql.SQLDatabaseProvider;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import org.h2.Driver;

import java.io.File;
import java.sql.*;
import java.util.Collection;
import java.util.Map;

public final class H2DatabaseProvider extends SQLDatabaseProvider {

    private static final long NEW_CREATION_DELAY = 600000;

    static {
        Driver.load();
    }

    protected final NetorHashMap<String, Long, H2Database> cachedDatabaseInstances = new NetorHashMap<>();
    protected final ITaskScheduler taskScheduler;
    protected final boolean autoShutdownTaskScheduler;
    protected final File h2dbFile;
    protected final boolean runsInCluster;
    protected Connection connection;

    public H2DatabaseProvider(String h2File, boolean runsInCluster) {
        this(h2File, runsInCluster, null);
    }

    public H2DatabaseProvider(String h2File, boolean runsInCluster, ITaskScheduler taskScheduler) {
        if (taskScheduler != null) {
            this.taskScheduler = taskScheduler;
            this.autoShutdownTaskScheduler = false;
        } else {
            this.taskScheduler = new DefaultTaskScheduler(1);
            this.autoShutdownTaskScheduler = true;
        }

        this.h2dbFile = new File(h2File);
        this.runsInCluster = runsInCluster;
    }

    @Override
    public boolean init() throws Exception {
        this.h2dbFile.getParentFile().mkdirs();
        this.connection = DriverManager.getConnection("jdbc:h2:" + this.h2dbFile.getAbsolutePath());

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
        Validate.checkNotNull(name);

        this.removedOutdatedEntries();

        if (!this.cachedDatabaseInstances.contains(name)) {
            this.cachedDatabaseInstances.add(name, System.currentTimeMillis() + NEW_CREATION_DELAY, new H2Database(this, name));
        }

        return this.cachedDatabaseInstances.getSecond(name);
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

        try (PreparedStatement preparedStatement = this.connection.prepareStatement("DROP TABLE " + name)) {
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
        return "h2";
    }

    @Override
    public void close() throws Exception {
        if (this.autoShutdownTaskScheduler) {
            this.taskScheduler.shutdown();
        }

        if (this.connection != null) {
            this.connection.close();
        }
    }

    private void removedOutdatedEntries() {
        for (Map.Entry<String, Pair<Long, H2Database>> entry : this.cachedDatabaseInstances.entrySet()) {
            if (entry.getValue().getFirst() < System.currentTimeMillis()) {
                this.cachedDatabaseInstances.remove(entry.getKey());
            }
        }
    }

    @Override
    public Connection getConnection() {
        return this.connection;
    }

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