package de.dytanic.cloudnet.ext.database.mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.AbstractDatabaseProvider;
import de.dytanic.cloudnet.driver.database.Database;
import de.dytanic.cloudnet.ext.database.mongodb.util.DatabaseConnectionData;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Getter
public class MongoDatabaseProvider extends AbstractDatabaseProvider {

    private MongoClient client;
    private JsonDocument config;
    private DatabaseConnectionData databaseConnectionData;
    private ExecutorService executorService;

    public MongoDatabaseProvider(JsonDocument config, ExecutorService executorService) {
        this.config = config;
        this.executorService = executorService;
        this.databaseConnectionData = DatabaseConnectionData.fromJsonDocument(config);
    }

    public void closeSession() {
        try {
            getClient().close();
        } catch (Exception e) {
        }
    }

    public boolean isConnected() {
        return getClient() != null;
    }

    @Override
    public boolean init() throws Exception {
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (!isConnected()) {
                    try {
                        closeSession();
                    } catch (Exception ex) {
                    }
                    connect();
                }
            }

        }, 0, 3, TimeUnit.MINUTES);
        return true;
    }

    public void connect() {
        try {
            this.client = MongoClients.create(new ConnectionString(this.databaseConnectionData.toConnectionString()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public String getName() {
        return "MongoDB-Provider";
    }

    public MongoDatabase getMongoDatabase() {
        if (getClient() == null) {
            connect();
        }
        return getClient().getDatabase(getDatabaseConnectionData().getDatabase());
    }

    @Override
    public Database getDatabase(String name) {
        return new de.dytanic.cloudnet.ext.database.mongodb.MongoDatabase(this, name, executorService);
    }

    @Override
    public boolean containsDatabase(String name) {
        return client.getDatabase(name) != null;
    }

    @Override
    public @NotNull ITask<Boolean> containsDatabaseAsync(String name) {
        return this.schedule(() -> containsDatabase(name));
    }

    @Override
    public @NotNull ITask<Boolean> deleteDatabaseAsync(String name) {
        return this.schedule(() -> deleteDatabase(name));
    }

    @Override
    public @NotNull ITask<Collection<String>> getDatabaseNamesAsync() {
        return this.schedule(() -> getDatabaseNames());
    }

    private <T> ITask<T> schedule(Callable<T> callable) {
        ITask<T> task = new ListenableTask<>(callable);
        this.executorService.execute(() -> {
            try {
                Thread.sleep(0, 100000);
                task.call();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });
        return task;
    }

    public boolean deleteDatabase(String name) {
        if (client.getDatabase(name) != null) {
            client.getDatabase(name).drop();
            return true;
        }
        return false;
    }

    @Override
    public Collection<String> getDatabaseNames() {
        Collection<String> names = new ArrayList<>();
        client.getDatabase(this.getDatabaseConnectionData().getDatabase()).listCollectionNames().forEach(n -> {
            names.add(n);
        });
        return names;
    }

    @Override
    public void close() throws Exception {
        if (client != null) {
            client.close();
        }
    }

}