package de.dytanic.cloudnet.ext.database.mysql;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.database.AbstractDatabaseProvider;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.ext.database.mysql.util.MySQLConnectionEndpoint;
import de.dytanic.cloudnet.module.NodeCloudNetModule;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public final class CloudNetMySQLDatabaseModule extends NodeCloudNetModule {

    public static final Type TYPE = new TypeToken<List<MySQLConnectionEndpoint>>() {
    }.getType();

    private static CloudNetMySQLDatabaseModule instance;

    public static CloudNetMySQLDatabaseModule getInstance() {
        return CloudNetMySQLDatabaseModule.instance;
    }

    @ModuleTask(order = 127, event = ModuleLifeCycle.LOADED)
    public void init() {
        instance = this;
    }

    @ModuleTask(order = 126, event = ModuleLifeCycle.LOADED)
    public void initConfig() {
        this.getConfig().getString("database", "mysql");
        this.getConfig().get("addresses", TYPE, Collections.singletonList(
                new MySQLConnectionEndpoint(false, "CloudNet", new HostAndPort("127.0.0.1", 3306))
        ));

        this.getConfig().getString("username", "root");
        this.getConfig().getString("password", "root");
        this.getConfig().getInt("connectionPoolSize", 15);
        this.getConfig().getInt("connectionTimeout", 5000);
        this.getConfig().getInt("validationTimeout", 5000);

        this.saveConfig();
    }

    @ModuleTask(order = 125, event = ModuleLifeCycle.LOADED)
    public void registerDatabaseProvider() {
        this.getRegistry().registerService(AbstractDatabaseProvider.class, this.getConfig().getString("database"), new MySQLDatabaseProvider(this.getConfig(), null));
    }

    @ModuleTask(order = 127, event = ModuleLifeCycle.STOPPED)
    public void unregisterDatabaseProvider() {
        this.getRegistry().unregisterService(AbstractDatabaseProvider.class, this.getConfig().getString("database"));
    }
}