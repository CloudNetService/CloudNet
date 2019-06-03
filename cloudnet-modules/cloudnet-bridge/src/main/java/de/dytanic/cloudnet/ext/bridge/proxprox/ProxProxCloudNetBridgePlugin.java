package de.dytanic.cloudnet.ext.bridge.proxprox;

import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.ProxyFallbackConfiguration;
import de.dytanic.cloudnet.ext.bridge.listener.BridgeCustomChannelMessageListener;
import de.dytanic.cloudnet.ext.bridge.proxprox.command.CommandCloudNet;
import de.dytanic.cloudnet.ext.bridge.proxprox.listener.ProxProxCloudNetListener;
import de.dytanic.cloudnet.ext.bridge.proxprox.listener.ProxProxPlayerListener;
import de.dytanic.cloudnet.wrapper.Wrapper;
import io.gomint.proxprox.api.plugin.Plugin;
import io.gomint.proxprox.api.plugin.annotation.Description;
import io.gomint.proxprox.api.plugin.annotation.Name;
import io.gomint.proxprox.api.plugin.annotation.Version;
import io.gomint.proxprox.api.scheduler.Task;
import io.gomint.proxprox.config.ServerConfig;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Name("CloudNet-Bridge")
@Version(major = 1, minor = 0)
@Description("ProxProx extension for the CloudNet runtime, which optimize some features")
public final class ProxProxCloudNetBridgePlugin extends Plugin {

    private volatile Task task;

    @Override
    public void onStartup() {
        this.initListeners();
        this.registerCommands();
        this.initServers();

        task = getScheduler().scheduleSync(this::updateProxProxDefaultServer, 0, 250, TimeUnit.MILLISECONDS);
        BridgeHelper.updateServiceInfo();
    }

    @Override
    public void onUninstall() {
        if (task != null) task.cancel();

        CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
        Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());
    }

    private void registerCommands() {
        registerCommand(new CommandCloudNet());
    }

    private void initListeners() {
        //ProxProx API
        registerListener(new ProxProxPlayerListener());

        //CloudNet
        CloudNetDriver.getInstance().getEventManager().registerListener(new ProxProxCloudNetListener());
        CloudNetDriver.getInstance().getEventManager().registerListener(new BridgeCustomChannelMessageListener());
    }

    private void initServers() {
        for (ServiceInfoSnapshot serviceInfoSnapshot : CloudNetDriver.getInstance().getCloudServices())
            if (serviceInfoSnapshot.getServiceId().getEnvironment().isMinecraftBedrockServer()) {
                if ((serviceInfoSnapshot.getProperties().contains("Online-Mode") && serviceInfoSnapshot.getProperties().getBoolean("Online-Mode")) ||
                        serviceInfoSnapshot.getLifeCycle() != ServiceLifeCycle.RUNNING)
                    continue;

                String name = serviceInfoSnapshot.getServiceId().getName();
                ProxProxCloudNetHelper.SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION.put(name, serviceInfoSnapshot);
            }
    }

    private void updateProxProxDefaultServer() {
        for (ProxyFallbackConfiguration proxyFallbackConfiguration : BridgeConfigurationProvider.load().getBungeeFallbackConfigurations())
            if (proxyFallbackConfiguration.getTargetGroup() != null && Iterables.contains(
                    proxyFallbackConfiguration.getTargetGroup(),
                    Wrapper.getInstance().getCurrentServiceInfoSnapshot().getConfiguration().getGroups()
            )) {
                Map.Entry<String, ServiceInfoSnapshot> server = null;

                List<Map.Entry<String, ServiceInfoSnapshot>> entries = ProxProxCloudNetHelper.getFilteredEntries(proxyFallbackConfiguration.getDefaultFallbackTask(), null);

                if (entries.size() > 0)
                    server = entries.get(new Random().nextInt(entries.size()));

                if (server != null) {
                    ProxProxCloudNetHelper.getProxyServer().getConfig().setDefaultServer(
                            new ServerConfig(server.getValue().getAddress().getHost(), server.getValue().getAddress().getPort())
                    );
                    return;
                }
            }
    }
}