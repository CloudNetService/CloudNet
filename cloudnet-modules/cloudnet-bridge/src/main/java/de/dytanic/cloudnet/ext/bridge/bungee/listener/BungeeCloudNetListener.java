package de.dytanic.cloudnet.ext.bridge.bungee.listener;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.event.events.network.NetworkChannelPacketReceiveEvent;
import de.dytanic.cloudnet.driver.event.events.service.*;
import de.dytanic.cloudnet.ext.bridge.bungee.BungeeCloudNetHelper;
import de.dytanic.cloudnet.ext.bridge.bungee.event.*;
import de.dytanic.cloudnet.ext.bridge.event.*;
import de.dytanic.cloudnet.ext.bridge.proxy.BridgeProxyHelper;
import de.dytanic.cloudnet.wrapper.event.service.ServiceInfoSnapshotConfigureEvent;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Event;

import java.net.InetSocketAddress;

public final class BungeeCloudNetListener {

    @EventListener
    public void handle(ServiceInfoSnapshotConfigureEvent event) {
        BungeeCloudNetHelper.initProperties(event.getServiceInfoSnapshot());
        this.bungeeCall(new BungeeServiceInfoSnapshotConfigureEvent(event.getServiceInfoSnapshot()));
    }

    @EventListener
    public void handle(CloudServiceStartEvent event) {
        if (BungeeCloudNetHelper.isServiceEnvironmentTypeProvidedForBungeeCord(event.getServiceInfo())) {
            if (event.getServiceInfo().getProperties().contains("Online-Mode") && event.getServiceInfo().getProperties().getBoolean("Online-Mode")) {
                return;
            }

            String name = event.getServiceInfo().getServiceId().getName();

            ProxyServer.getInstance().getServers().put(name, BungeeCloudNetHelper.createServerInfo(name, new InetSocketAddress(
                    event.getServiceInfo().getAddress().getHost(),
                    event.getServiceInfo().getAddress().getPort()
            )));
        }

        this.bungeeCall(new BungeeCloudServiceStartEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceStopEvent event) {
        if (BungeeCloudNetHelper.isServiceEnvironmentTypeProvidedForBungeeCord(event.getServiceInfo())) {
            String name = event.getServiceInfo().getServiceId().getName();
            ProxyServer.getInstance().getServers().remove(name);
        }

        this.bungeeCall(new BungeeCloudServiceStopEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceInfoUpdateEvent event) {
        if (BungeeCloudNetHelper.isServiceEnvironmentTypeProvidedForBungeeCord(event.getServiceInfo())) {
            BridgeProxyHelper.cacheServiceInfoSnapshot(event.getServiceInfo());
        }

        this.bungeeCall(new BungeeCloudServiceInfoUpdateEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceRegisterEvent event) {
        if (BungeeCloudNetHelper.isServiceEnvironmentTypeProvidedForBungeeCord(event.getServiceInfo())) {
            BridgeProxyHelper.cacheServiceInfoSnapshot(event.getServiceInfo());
        }

        this.bungeeCall(new BungeeCloudServiceRegisterEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceConnectNetworkEvent event) {
        if (BungeeCloudNetHelper.isServiceEnvironmentTypeProvidedForBungeeCord(event.getServiceInfo())) {
            BridgeProxyHelper.cacheServiceInfoSnapshot(event.getServiceInfo());
        }

        this.bungeeCall(new BungeeCloudServiceConnectNetworkEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceDisconnectNetworkEvent event) {
        if (BungeeCloudNetHelper.isServiceEnvironmentTypeProvidedForBungeeCord(event.getServiceInfo())) {
            BridgeProxyHelper.cacheServiceInfoSnapshot(event.getServiceInfo());
        }

        this.bungeeCall(new BungeeCloudServiceDisconnectNetworkEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceUnregisterEvent event) {
        if (BungeeCloudNetHelper.isServiceEnvironmentTypeProvidedForBungeeCord(event.getServiceInfo())) {
            BridgeProxyHelper.cacheServiceInfoSnapshot(event.getServiceInfo());
        }

        this.bungeeCall(new BungeeCloudServiceUnregisterEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(ChannelMessageReceiveEvent event) {
        this.bungeeCall(new BungeeChannelMessageReceiveEvent(event));
    }

    @EventListener
    public void handle(NetworkChannelPacketReceiveEvent event) {
        this.bungeeCall(new BungeeNetworkChannelPacketReceiveEvent(event.getChannel(), event.getPacket()));
    }

    @EventListener
    public void handle(BridgeConfigurationUpdateEvent event) {
        this.bungeeCall(new BungeeBridgeConfigurationUpdateEvent(event.getBridgeConfiguration()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerLoginRequestEvent event) {
        this.bungeeCall(new BungeeBridgeProxyPlayerLoginRequestEvent(event.getNetworkConnectionInfo()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerLoginSuccessEvent event) {
        this.bungeeCall(new BungeeBridgeProxyPlayerLoginSuccessEvent(event.getNetworkConnectionInfo()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerServerConnectRequestEvent event) {
        this.bungeeCall(new BungeeBridgeProxyPlayerServerConnectRequestEvent(event.getNetworkConnectionInfo(), event.getNetworkServiceInfo()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerServerSwitchEvent event) {
        this.bungeeCall(new BungeeBridgeProxyPlayerServerSwitchEvent(event.getNetworkConnectionInfo(), event.getNetworkServiceInfo()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerDisconnectEvent event) {
        this.bungeeCall(new BungeeBridgeProxyPlayerDisconnectEvent(event.getNetworkConnectionInfo()));
    }

    @EventListener
    public void handle(BridgeServerPlayerLoginRequestEvent event) {
        this.bungeeCall(new BungeeBridgeServerPlayerLoginRequestEvent(event.getNetworkConnectionInfo(), event.getNetworkPlayerServerInfo()));
    }

    @EventListener
    public void handle(BridgeServerPlayerLoginSuccessEvent event) {
        this.bungeeCall(new BungeeBridgeServerPlayerLoginSuccessEvent(event.getNetworkConnectionInfo(), event.getNetworkPlayerServerInfo()));
    }

    @EventListener
    public void handle(BridgeServerPlayerDisconnectEvent event) {
        this.bungeeCall(new BungeeBridgeServerPlayerDisconnectEvent(event.getNetworkConnectionInfo(), event.getNetworkPlayerServerInfo()));
    }

    private void bungeeCall(Event event) {
        ProxyServer.getInstance().getPluginManager().callEvent(event);
    }

}
