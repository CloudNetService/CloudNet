package de.dytanic.cloudnet.ext.bridge.bukkit.listener;

import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.concurrent.CompletedTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.event.events.network.NetworkChannelPacketReceiveEvent;
import de.dytanic.cloudnet.driver.event.events.service.*;
import de.dytanic.cloudnet.ext.bridge.bukkit.BukkitCloudNetHelper;
import de.dytanic.cloudnet.ext.bridge.bukkit.event.*;
import de.dytanic.cloudnet.ext.bridge.event.*;
import de.dytanic.cloudnet.wrapper.event.service.ServiceInfoSnapshotConfigureEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.ExecutionException;

public final class BukkitCloudNetListener {

    private final Plugin plugin;

    public BukkitCloudNetListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventListener
    public void handle(ServiceInfoSnapshotConfigureEvent event) throws ExecutionException, InterruptedException {
        BukkitCloudNetHelper.initProperties(event.getServiceInfoSnapshot());
        this.listenableBukkitCall(new BukkitServiceInfoSnapshotConfigureEvent(event.getServiceInfoSnapshot())).get();
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
    public void handle(ChannelMessageReceiveEvent event) throws InterruptedException, ExecutionException {
        this.listenableBukkitCall(new BukkitChannelMessageReceiveEvent(event)).get();
    }

    @EventListener
    public void handle(NetworkChannelPacketReceiveEvent event) throws ExecutionException, InterruptedException {
        this.listenableBukkitCall(new BukkitNetworkChannelPacketReceiveEvent(event.getChannel(), event.getPacket())).get();
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
        this.bukkitSyncExecution(() -> Bukkit.getPluginManager().callEvent(event));
    }

    private ITask<Void> listenableBukkitCall(Event event) {
        return event.getHandlers().getRegisteredListeners().length == 0
                ? CompletedTask.voidTask()
                : this.listenableBukkitSyncExecution(() -> Bukkit.getPluginManager().callEvent(event));
    }

    private void bukkitSyncExecution(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
            return;
        }

        Bukkit.getScheduler().runTask(this.plugin, runnable);
    }

    private ITask<Void> listenableBukkitSyncExecution(Runnable runnable) {
        CompletableTask<Void> task = new CompletableTask<>();
        this.bukkitSyncExecution(() -> {
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