package de.dytanic.cloudnet.ext.bridge.bungee;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.BridgePlayerManager;
import de.dytanic.cloudnet.ext.bridge.bungee.command.CommandCloudNet;
import de.dytanic.cloudnet.ext.bridge.bungee.command.CommandHub;
import de.dytanic.cloudnet.ext.bridge.bungee.listener.BungeeCloudNetListener;
import de.dytanic.cloudnet.ext.bridge.bungee.listener.BungeePlayerExecutorListener;
import de.dytanic.cloudnet.ext.bridge.bungee.listener.BungeePlayerListener;
import de.dytanic.cloudnet.ext.bridge.listener.BridgeCustomChannelMessageListener;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import de.dytanic.cloudnet.ext.bridge.proxy.BridgeProxyHelper;
import de.dytanic.cloudnet.wrapper.Wrapper;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.plugin.Plugin;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public final class BungeeCloudNetBridgePlugin extends Plugin {

    @Override
    public synchronized void onEnable() {
        CloudNetDriver.getInstance().getServicesRegistry().registerService(IPlayerManager.class, "BridgePlayerManager", new BridgePlayerManager());

        this.initListeners();
        this.registerCommands();
        this.initServers();
        this.runPlayerDisconnectTask();

        this.getProxy().setReconnectHandler(new BungeeCloudNetReconnectHandler());
        for (ListenerInfo listenerInfo : this.getProxy().getConfig().getListeners()) {
            listenerInfo.getServerPriority().clear();
        }

        BridgeHelper.updateServiceInfo();
    }

    @Override
    public synchronized void onDisable() {
        CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
        Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());
    }

    private void runPlayerDisconnectTask() {
        super.getProxy().getScheduler().schedule(this, () -> {
            if (BungeeCloudNetHelper.getLastOnlineCount() != -1 &&
                    super.getProxy().getPlayers().size() != BungeeCloudNetHelper.getLastOnlineCount()) {
                Wrapper.getInstance().publishServiceInfoUpdate();
            }
        }, 500, 500, TimeUnit.MILLISECONDS);
    }

    private void initServers() {
        for (ServiceInfoSnapshot serviceInfoSnapshot : CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServices()) {
            if (BungeeCloudNetHelper.isServiceEnvironmentTypeProvidedForBungeeCord(serviceInfoSnapshot)) {
                if ((serviceInfoSnapshot.getProperties().contains("Online-Mode") && serviceInfoSnapshot.getProperties().getBoolean("Online-Mode")) ||
                        serviceInfoSnapshot.getLifeCycle() != ServiceLifeCycle.RUNNING) {
                    continue;
                }

                String name = serviceInfoSnapshot.getServiceId().getName();

                super.getProxy().getServers().put(name, BungeeCloudNetHelper.createServerInfo(name, new InetSocketAddress(
                        serviceInfoSnapshot.getAddress().getHost(),
                        serviceInfoSnapshot.getAddress().getPort()
                )));

                BridgeProxyHelper.cacheServiceInfoSnapshot(serviceInfoSnapshot);
            }
        }
    }

    private void registerCommands() {
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new CommandCloudNet());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new CommandHub());
    }

    private void initListeners() {
        //BungeeCord API
        ProxyServer.getInstance().getPluginManager().registerListener(this, new BungeePlayerListener(this));

        //CloudNet
        CloudNetDriver.getInstance().getEventManager().registerListener(new BungeeCloudNetListener());
        CloudNetDriver.getInstance().getEventManager().registerListener(new BungeePlayerExecutorListener());
        CloudNetDriver.getInstance().getEventManager().registerListener(new BridgeCustomChannelMessageListener());
    }
}