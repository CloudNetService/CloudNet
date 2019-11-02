package de.dytanic.cloudnet.ext.bridge.bukkit.listener;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.event.events.instance.CloudNetTickEvent;
import de.dytanic.cloudnet.driver.event.events.network.NetworkChannelPacketReceiveEvent;
import de.dytanic.cloudnet.driver.event.events.network.NetworkClusterNodeInfoUpdateEvent;
import de.dytanic.cloudnet.driver.event.events.service.*;
import de.dytanic.cloudnet.ext.bridge.bukkit.BukkitCloudNetHelper;
import de.dytanic.cloudnet.ext.bridge.bukkit.event.*;
import de.dytanic.cloudnet.ext.bridge.event.*;
import de.dytanic.cloudnet.wrapper.event.service.ServiceInfoSnapshotConfigureEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;

public final class BukkitCloudNetListener {

    @EventListener
    public void handle(ServiceInfoSnapshotConfigureEvent event) {
        BukkitCloudNetHelper.initProperties(event.getServiceInfoSnapshot());
        this.bukkitCall(new BukkitServiceInfoSnapshotConfigureEvent(event.getServiceInfoSnapshot()));
    }

    @EventListener
    public void handle(CloudServiceInfoUpdateEvent event) {
        this.bukkitCall(new BukkitCloudServiceInfoUpdateEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceRegisterEvent event) {
        this.bukkitCall(new BukkitCloudServiceRegisterEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceStartEvent event) {
        this.bukkitCall(new BukkitCloudServiceStartEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceConnectNetworkEvent event) {
        this.bukkitCall(new BukkitCloudServiceConnectNetworkEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceDisconnectNetworkEvent event) {
        this.bukkitCall(new BukkitCloudServiceDisconnectNetworkEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceStopEvent event) {
        this.bukkitCall(new BukkitCloudServiceStopEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceUnregisterEvent event) {
        this.bukkitCall(new BukkitCloudServiceUnregisterEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(ChannelMessageReceiveEvent event) {
        this.bukkitCall(new BukkitChannelMessageReceiveEvent(event.getChannel(), event.getMessage(), event.getData()));
    }

    @EventListener
    public void handle(CloudNetTickEvent event) {
        this.bukkitCall(new BukkitCloudNetTickEvent());
    }

    @EventListener
    public void handle(NetworkClusterNodeInfoUpdateEvent event) {
        this.bukkitCall(new BukkitNetworkClusterNodeInfoUpdateEvent(event.getNetworkClusterNodeInfoSnapshot()));
    }

    @EventListener
    public void handle(NetworkChannelPacketReceiveEvent event) {
        this.bukkitCall(new BukkitNetworkChannelPacketReceiveEvent(event.getChannel(), event.getPacket()));
    }

    @EventListener
    public void handle(BridgeConfigurationUpdateEvent event) {
        this.bukkitCall(new BukkitBridgeConfigurationUpdateEvent(event.getBridgeConfiguration()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerLoginRequestEvent event) {
        this.bukkitCall(new BukkitBridgeProxyPlayerLoginSuccessEvent(event.getNetworkConnectionInfo()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerLoginSuccessEvent event) {
        this.bukkitCall(new BukkitBridgeProxyPlayerLoginSuccessEvent(event.getNetworkConnectionInfo()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerServerConnectRequestEvent event) {
        this.bukkitCall(new BukkitBridgeProxyPlayerServerConnectRequestEvent(event.getNetworkConnectionInfo(), event.getNetworkServiceInfo()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerServerSwitchEvent event) {
        this.bukkitCall(new BukkitBridgeProxyPlayerServerSwitchEvent(event.getNetworkConnectionInfo(), event.getNetworkServiceInfo()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerDisconnectEvent event) {
        this.bukkitCall(new BukkitBridgeProxyPlayerDisconnectEvent(event.getNetworkConnectionInfo()));
    }

    @EventListener
    public void handle(BridgeServerPlayerLoginRequestEvent event) {
        this.bukkitCall(new BukkitBridgeServerPlayerLoginRequestEvent(event.getNetworkConnectionInfo(), event.getNetworkPlayerServerInfo()));
    }

    @EventListener
    public void handle(BridgeServerPlayerLoginSuccessEvent event) {
        this.bukkitCall(new BukkitBridgeServerPlayerLoginSuccessEvent(event.getNetworkConnectionInfo(), event.getNetworkPlayerServerInfo()));
    }

    @EventListener
    public void handle(BridgeServerPlayerDisconnectEvent event) {
        this.bukkitCall(new BukkitBridgeServerPlayerDisconnectEvent(event.getNetworkConnectionInfo(), event.getNetworkPlayerServerInfo()));
    }

    private void bukkitCall(Event event) {
        Bukkit.getScheduler().runTask(BukkitCloudNetHelper.getPlugin(), () -> Bukkit.getPluginManager().callEvent(event));
    }
}