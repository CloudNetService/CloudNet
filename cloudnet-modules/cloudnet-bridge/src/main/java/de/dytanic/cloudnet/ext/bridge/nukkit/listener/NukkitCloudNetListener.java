package de.dytanic.cloudnet.ext.bridge.nukkit.listener;

import cn.nukkit.Server;
import cn.nukkit.event.Event;
import cn.nukkit.plugin.Plugin;
import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.event.events.network.NetworkChannelPacketReceiveEvent;
import de.dytanic.cloudnet.driver.event.events.service.*;
import de.dytanic.cloudnet.ext.bridge.event.*;
import de.dytanic.cloudnet.ext.bridge.nukkit.NukkitCloudNetHelper;
import de.dytanic.cloudnet.ext.bridge.nukkit.event.*;
import de.dytanic.cloudnet.wrapper.event.service.ServiceInfoSnapshotConfigureEvent;

import java.util.concurrent.ExecutionException;

public final class NukkitCloudNetListener {

    private final Plugin plugin;

    public NukkitCloudNetListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventListener
    public void handle(ServiceInfoSnapshotConfigureEvent event) throws ExecutionException, InterruptedException {
        NukkitCloudNetHelper.initProperties(event.getServiceInfoSnapshot());
        this.listenableNukkitCall(new NukkitServiceInfoSnapshotConfigureEvent(event.getServiceInfoSnapshot())).get();
    }

    @EventListener
    public void handle(CloudServiceInfoUpdateEvent event) {
        this.nukkitCall(new NukkitCloudServiceInfoUpdateEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceRegisterEvent event) {
        this.nukkitCall(new NukkitCloudServiceRegisterEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceStartEvent event) {
        this.nukkitCall(new NukkitCloudServiceStartEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceConnectNetworkEvent event) {
        this.nukkitCall(new NukkitCloudServiceConnectNetworkEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceDisconnectNetworkEvent event) {
        this.nukkitCall(new NukkitCloudServiceDisconnectNetworkEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceStopEvent event) {
        this.nukkitCall(new NukkitCloudServiceStopEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceUnregisterEvent event) {
        this.nukkitCall(new NukkitCloudServiceUnregisterEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(ChannelMessageReceiveEvent event) throws ExecutionException, InterruptedException {
        this.listenableNukkitCall(new NukkitChannelMessageReceiveEvent(event)).get();
    }

    @EventListener
    public void handle(NetworkChannelPacketReceiveEvent event) throws ExecutionException, InterruptedException {
        this.listenableNukkitCall(new NukkitNetworkChannelPacketReceiveEvent(event.getChannel(), event.getPacket())).get();
    }

    @EventListener
    public void handle(BridgeConfigurationUpdateEvent event) {
        this.nukkitCall(new NukkitBridgeConfigurationUpdateEvent(event.getBridgeConfiguration()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerLoginRequestEvent event) {
        this.nukkitCall(new NukkitBridgeProxyPlayerLoginSuccessEvent(event.getNetworkConnectionInfo()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerLoginSuccessEvent event) {
        this.nukkitCall(new NukkitBridgeProxyPlayerLoginSuccessEvent(event.getNetworkConnectionInfo()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerServerConnectRequestEvent event) {
        this.nukkitCall(new NukkitBridgeProxyPlayerServerConnectRequestEvent(event.getNetworkConnectionInfo(), event.getNetworkServiceInfo()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerServerSwitchEvent event) {
        this.nukkitCall(new NukkitBridgeProxyPlayerServerSwitchEvent(event.getNetworkConnectionInfo(), event.getNetworkServiceInfo()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerDisconnectEvent event) {
        this.nukkitCall(new NukkitBridgeProxyPlayerDisconnectEvent(event.getNetworkConnectionInfo()));
    }

    @EventListener
    public void handle(BridgeServerPlayerLoginRequestEvent event) {
        this.nukkitCall(new NukkitBridgeServerPlayerLoginRequestEvent(event.getNetworkConnectionInfo(), event.getNetworkPlayerServerInfo()));
    }

    @EventListener
    public void handle(BridgeServerPlayerLoginSuccessEvent event) {
        this.nukkitCall(new NukkitBridgeServerPlayerLoginSuccessEvent(event.getNetworkConnectionInfo(), event.getNetworkPlayerServerInfo()));
    }

    @EventListener
    public void handle(BridgeServerPlayerDisconnectEvent event) {
        this.nukkitCall(new NukkitBridgeServerPlayerDisconnectEvent(event.getNetworkConnectionInfo(), event.getNetworkPlayerServerInfo()));
    }

    private void nukkitCall(Event event) {
        this.nukkitSyncExecution(() -> Server.getInstance().getPluginManager().callEvent(event));
    }

    private ITask<Void> listenableNukkitCall(Event event) {
        return this.listenableNukkitSyncExecution(() -> Server.getInstance().getPluginManager().callEvent(event));
    }

    private void nukkitSyncExecution(Runnable runnable) {
        if (Server.getInstance().isPrimaryThread()) {
            runnable.run();
            return;
        }

        Server.getInstance().getScheduler().scheduleTask(this.plugin, runnable);
    }

    private ITask<Void> listenableNukkitSyncExecution(Runnable runnable) {
        CompletableTask<Void> task = new CompletableTask<>();
        this.nukkitSyncExecution(() -> {
            runnable.run();
            try {
                task.complete(null);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });
        return task;
    }

}