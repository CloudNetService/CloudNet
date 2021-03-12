package de.dytanic.cloudnet.ext.database.mongodb;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.AbstractDatabaseProvider;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.ext.database.mongodb.command.SetupCommand;
import de.dytanic.cloudnet.module.NodeCloudNetModule;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.Executors;
import lombok.Getter;

@Getter
public class CloudNetMongoDBDatabaseModule extends NodeCloudNetModule {

    public static final Type TYPE = new TypeToken<List<String>>() {
    }.getType();

    private static CloudNetMongoDBDatabaseModule instance;
    private MongoDatabaseProvider mongoDatabaseProvider;

    @ModuleTask(order = 127, event = ModuleLifeCycle.LOADED)
    public void init() {
        instance = this;
    }


    @ModuleTask(order = 126, event = ModuleLifeCycle.LOADED)
    public void initConfig() {
        if (getConfig().isEmpty()) {
            JsonDocument database = new JsonDocument();
            database.getString("host", (String) "localhost");
            database.getInt("port", (Integer) 27017);
            database.getString("database", (String) "cloudnet");
            database.getString("user", (String) "cloudnet");
            database.getString("password", (String) "password");
            getConfig().getDocument("connection", database);
            saveConfig();
        }
    }

    @ModuleTask(event = ModuleLifeCycle.STARTED)
    public void registerSetupCommand() {
        registerCommand(new SetupCommand(this));
    }


    @ModuleTask(order = 125, event = ModuleLifeCycle.LOADED)
    public void registerDatabaseProvider() {
        this.mongoDatabaseProvider = new MongoDatabaseProvider(getConfig().getDocument("connection"), Executors.newSingleThreadExecutor());
        getRegistry().registerService(AbstractDatabaseProvider.class, "mongodb", mongoDatabaseProvider);
    }

    @ModuleTask(order = 127, event = ModuleLifeCycle.UNLOADED)
    public void unregisterDatabaseProvider() {
        if (mongoDatabaseProvider != null) {
            mongoDatabaseProvider.closeSession();
        }
        getRegistry().unregisterService(AbstractDatabaseProvider.class, "mongodb");
    }

}
