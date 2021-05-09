package de.dytanic.cloudnet.ext.cloudperms.node;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.ext.cloudperms.node.listener.ConfigurationUpdateListener;
import de.dytanic.cloudnet.ext.cloudperms.node.listener.IncludePluginListener;
import de.dytanic.cloudnet.module.NodeCloudNetModule;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public final class CloudNetCloudPermissionsModule extends NodeCloudNetModule {

    private static final Type LIST_STRING = new TypeToken<List<String>>() {
    }.getType();

    private static CloudNetCloudPermissionsModule instance;

    public static CloudNetCloudPermissionsModule getInstance() {
        return CloudNetCloudPermissionsModule.instance;
    }

    @ModuleTask(order = 127, event = ModuleLifeCycle.LOADED)
    public void init() {
        instance = this;
    }

    @ModuleTask(order = 126, event = ModuleLifeCycle.STARTED)
    public void initConfig() {
        this.getConfig().getBoolean("enabled", true);
        this.getConfig().get("excludedGroups", LIST_STRING, new ArrayList<>());
        this.saveConfig();
    }

    @ModuleTask(order = 124, event = ModuleLifeCycle.STARTED)
    public void registerListeners() {
        this.registerListeners(new IncludePluginListener(), new ConfigurationUpdateListener());
    }

    public List<String> getExcludedGroups() {
        return this.getConfig().get("excludedGroups", LIST_STRING);
    }
}
