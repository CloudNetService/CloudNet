package de.dytanic.cloudnet.ext.bridge.bungee;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.bungee.command.CommandCloudNet;
import de.dytanic.cloudnet.ext.bridge.bungee.command.CommandHub;
import de.dytanic.cloudnet.ext.bridge.bungee.listener.BungeeCloudNetListener;
import de.dytanic.cloudnet.ext.bridge.bungee.listener.BungeePlayerListener;
import de.dytanic.cloudnet.ext.bridge.listener.BridgeCustomChannelMessageListener;
import de.dytanic.cloudnet.wrapper.Wrapper;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

import java.net.InetSocketAddress;

public final class BungeeCloudNetBridgePlugin extends Plugin {

    @Override
    public synchronized void onEnable() {
        this.initListeners();
        this.registerCommands();
        this.initServers();

        BridgeHelper.updateServiceInfo();
    }

    @Override
    public synchronized void onDisable() {
        CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
        Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());
    }

    /*= ----------------------------------------------------------- =*/

    private void initServers() {
        for (ServiceInfoSnapshot serviceInfoSnapshot : CloudNetDriver.getInstance().getCloudServices())
            if (serviceInfoSnapshot.getServiceId().getEnvironment().isMinecraftJavaServer()) {
                if ((serviceInfoSnapshot.getProperties().contains("Online-Mode") && serviceInfoSnapshot.getProperties().getBoolean("Online-Mode")) ||
                        serviceInfoSnapshot.getLifeCycle() != ServiceLifeCycle.RUNNING)
                    continue;

                String name = serviceInfoSnapshot.getServiceId().getName();

                this.getProxy().getServers().put(name, BungeeCloudNetHelper.createServerInfo(name, new InetSocketAddress(
                        serviceInfoSnapshot.getAddress().getHost(),
                        serviceInfoSnapshot.getAddress().getPort()
                )));

                BungeeCloudNetHelper.SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION.put(name, serviceInfoSnapshot);
                BungeeCloudNetHelper.addItemToBungeeCordListenerPrioritySystem(serviceInfoSnapshot, name);
            }
    }

    private void registerCommands() {
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new CommandCloudNet());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new CommandHub());
    }

    private void initListeners() {
        //BungeeCord API
        ProxyServer.getInstance().getPluginManager().registerListener(this, new BungeePlayerListener());

        //CloudNet
        CloudNetDriver.getInstance().getEventManager().registerListener(new BungeeCloudNetListener());
        CloudNetDriver.getInstance().getEventManager().registerListener(new BridgeCustomChannelMessageListener());
    }
}