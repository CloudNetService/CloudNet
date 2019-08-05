package de.dytanic.cloudnet.ext.bridge.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.ext.bridge.listener.BridgeCustomChannelMessageListener;
import de.dytanic.cloudnet.ext.bridge.velocity.command.CommandCloudNet;
import de.dytanic.cloudnet.ext.bridge.velocity.command.CommandHub;
import de.dytanic.cloudnet.ext.bridge.velocity.listener.VelocityCloudNetListener;
import de.dytanic.cloudnet.ext.bridge.velocity.listener.VelocityPlayerListener;
import de.dytanic.cloudnet.wrapper.Wrapper;

import java.net.InetSocketAddress;

@Plugin(id = "cloudnet_bridge_velocity")
public final class VelocityCloudNetBridgePlugin {

    private static VelocityCloudNetBridgePlugin instance;

    private final ProxyServer proxyServer;

    @Inject
    public VelocityCloudNetBridgePlugin(ProxyServer proxyServer) {
        instance = this;

        this.proxyServer = proxyServer;
        VelocityCloudNetHelper.setProxyServer(proxyServer);
    }

    public static VelocityCloudNetBridgePlugin getInstance() {
        return VelocityCloudNetBridgePlugin.instance;
    }

    @Subscribe
    public void handleProxyInit(ProxyInitializeEvent event) {
        initListeners();
        registerCommands();
        initServers();
    }

    @Subscribe
    public void handleShutdown(ProxyShutdownEvent event) {
        CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
        Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());
    }

    private void initListeners() {
        //Velocity API
        this.proxyServer.getEventManager().register(this, new VelocityPlayerListener());

        //CloudNet
        CloudNetDriver.getInstance().getEventManager().registerListener(new VelocityCloudNetListener());
        CloudNetDriver.getInstance().getEventManager().registerListener(new BridgeCustomChannelMessageListener());
    }

    private void registerCommands() {
        this.proxyServer.getCommandManager().register(new CommandCloudNet(), "cloudnet", "cloud", "cl");
        this.proxyServer.getCommandManager().register(new CommandHub(), "hub", "l", "leave", "lobby");
    }

    private void initServers() {
        for (ServiceInfoSnapshot serviceInfoSnapshot : CloudNetDriver.getInstance().getCloudServices()) {
            if (serviceInfoSnapshot.getServiceId().getEnvironment().isMinecraftJavaServer()) {
                if ((serviceInfoSnapshot.getProperties().contains("Online-Mode") && serviceInfoSnapshot.getProperties().getBoolean("Online-Mode")) ||
                        serviceInfoSnapshot.getLifeCycle() != ServiceLifeCycle.RUNNING) {
                    continue;
                }

                String name = serviceInfoSnapshot.getServiceId().getName();
                proxyServer.registerServer(new ServerInfo(name, new InetSocketAddress(
                        serviceInfoSnapshot.getAddress().getHost(),
                        serviceInfoSnapshot.getAddress().getPort()
                )));

                VelocityCloudNetHelper.SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION.put(name, serviceInfoSnapshot);
                VelocityCloudNetHelper.addServerToVelocityPrioritySystemConfiguration(serviceInfoSnapshot, name);
            }
        }
    }

    public ProxyServer getProxyServer() {
        return this.proxyServer;
    }
}