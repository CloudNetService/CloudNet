package de.dytanic.cloudnet.ext.syncproxy.node;

import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyConfigurationWriterAndReader;
import de.dytanic.cloudnet.ext.syncproxy.node.command.CommandSyncProxy;
import de.dytanic.cloudnet.ext.syncproxy.node.http.V1SyncProxyConfigurationHttpHandler;
import de.dytanic.cloudnet.ext.syncproxy.node.listener.IncludePluginListener;
import de.dytanic.cloudnet.ext.syncproxy.node.listener.SyncProxyConfigUpdateListener;
import de.dytanic.cloudnet.ext.syncproxy.node.listener.SyncProxyDefaultConfigurationListener;
import de.dytanic.cloudnet.module.NodeCloudNetModule;

import java.io.File;

public final class CloudNetSyncProxyModule extends NodeCloudNetModule {

    private static CloudNetSyncProxyModule instance;

    private SyncProxyConfiguration syncProxyConfiguration;

    private File configurationFile;

    public CloudNetSyncProxyModule() {
        instance = this;
    }

    public static CloudNetSyncProxyModule getInstance() {
        return CloudNetSyncProxyModule.instance;
    }

    @ModuleTask(order = 127, event = ModuleLifeCycle.STARTED)
    public void createConfigurationOrUpdate() {
        configurationFile = new File(getModuleWrapper().getDataFolder(), "config.json");
        syncProxyConfiguration = SyncProxyConfigurationWriterAndReader.read(configurationFile);
    }

    @ModuleTask(order = 64, event = ModuleLifeCycle.STARTED)
    public void initListeners() {
        registerListeners(new IncludePluginListener(), new SyncProxyConfigUpdateListener(), new SyncProxyDefaultConfigurationListener());
    }

    @ModuleTask(order = 60, event = ModuleLifeCycle.STARTED)
    public void registerCommands() {
        registerCommand(new CommandSyncProxy());
    }

    @ModuleTask(order = 35, event = ModuleLifeCycle.STARTED)
    public void registerHttpHandlers() {
        getCloudNet().getHttpServer().registerHandler("/api/v1/modules/syncproxy/config",
                new V1SyncProxyConfigurationHttpHandler("cloudnet.http.v1.modules.syncproxy.config"));
    }

    public SyncProxyConfiguration getSyncProxyConfiguration() {
        return this.syncProxyConfiguration;
    }

    public void setSyncProxyConfiguration(SyncProxyConfiguration syncProxyConfiguration) {
        this.syncProxyConfiguration = syncProxyConfiguration;
    }

    public File getConfigurationFile() {
        return this.configurationFile;
    }
}