package de.dytanic.cloudnet.ext.database.mysql;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.database.AbstractDatabaseProvider;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.ext.database.mysql.util.MySQLConnectionEndpoint;
import de.dytanic.cloudnet.module.NodeCloudNetModule;
import lombok.Getter;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public final class CloudNetMySQLDatabaseModule extends NodeCloudNetModule {

    public static final Type TYPE = new TypeToken<List<MySQLConnectionEndpoint>>() {
    }.getType();

    @Getter
    private static CloudNetMySQLDatabaseModule instance;

    @ModuleTask(order = 127, event = ModuleLifeCycle.LOADED)
    public void init() {
        instance = this;
    }

    @ModuleTask(order = 126, event = ModuleLifeCycle.LOADED)
    public void initConfig() {
        getConfig().getString("database", "mysql");
        getConfig().get("addresses", TYPE, Collections.singletonList(
                new MySQLConnectionEndpoint(false, "CloudNet", new HostAndPort("127.0.0.1", 3306))
        ));

        getConfig().getString("username", "root");
        getConfig().getString("password", "root");
        getConfig().getInt("connectionPoolSize", 15);
        getConfig().getInt("connectionTimeout", 5000);
        getConfig().getInt("validationTimeout", 5000);

        saveConfig();
    }

    @ModuleTask(order = 125, event = ModuleLifeCycle.LOADED)
    public void registerDatabaseProvider() {
        getRegistry().registerService(AbstractDatabaseProvider.class, getConfig().getString("database"), new MySQLDatabaseProvider(getConfig()));
    }

    @ModuleTask(order = 127, event = ModuleLifeCycle.STOPPED)
    public void unregisterDatabaseProvider() {
        getRegistry().unregisterService(AbstractDatabaseProvider.class, getConfig().getString("database"));
    }
}