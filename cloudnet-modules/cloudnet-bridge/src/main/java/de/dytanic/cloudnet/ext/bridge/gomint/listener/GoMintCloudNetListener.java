package de.dytanic.cloudnet.ext.bridge.gomint.listener;

import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.event.events.network.NetworkChannelPacketReceiveEvent;
import de.dytanic.cloudnet.driver.event.events.service.*;
import de.dytanic.cloudnet.ext.bridge.event.*;
import de.dytanic.cloudnet.ext.bridge.gomint.GoMintCloudNetHelper;
import de.dytanic.cloudnet.ext.bridge.gomint.event.*;
import de.dytanic.cloudnet.wrapper.event.service.ServiceInfoSnapshotConfigureEvent;
import io.gomint.GoMint;
import io.gomint.event.Event;
import io.gomint.plugin.Plugin;

import java.util.concurrent.ExecutionException;

public final class GoMintCloudNetListener {

    private final Plugin plugin;

    public GoMintCloudNetListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventListener
    public void handle(ServiceInfoSnapshotConfigureEvent event) throws ExecutionException, InterruptedException {
        this.listenableGoMintSyncExecution(() -> {
            GoMintCloudNetHelper.initProperties(event.getServiceInfoSnapshot());
            GoMint.instance().getPluginManager().callEvent(new GoMintServiceInfoSnapshotConfigureEvent(event.getServiceInfoSnapshot()));
        }).get();
    }

    @EventListener
    public void handle(CloudServiceInfoUpdateEvent event) {
        this.goMintCall(new GoMintCloudServiceInfoUpdateEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceRegisterEvent event) {
        this.goMintCall(new GoMintCloudServiceRegisterEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceStartEvent event) {
        this.goMintCall(new GoMintCloudServiceStartEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceConnectNetworkEvent event) {
        this.goMintCall(new GoMintCloudServiceConnectNetworkEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceDisconnectNetworkEvent event) {
        this.goMintCall(new GoMintCloudServiceDisconnectNetworkEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceStopEvent event) {
        this.goMintCall(new GoMintCloudServiceStopEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceUnregisterEvent event) {
        this.goMintCall(new GoMintCloudServiceUnregisterEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(ChannelMessageReceiveEvent event) {
        this.goMintCall(new GoMintChannelMessageReceiveEvent(event));
    }

    @EventListener
    public void handle(NetworkChannelPacketReceiveEvent event) {
        this.goMintCall(new GoMintNetworkChannelPacketReceiveEvent(event.getChannel(), event.getPacket()));
    }

    @EventListener
    public void handle(BridgeConfigurationUpdateEvent event) {
        this.goMintCall(new GoMintBridgeConfigurationUpdateEvent(event.getBridgeConfiguration()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerLoginRequestEvent event) {
        this.goMintCall(new GoMintBridgeProxyPlayerLoginSuccessEvent(event.getNetworkConnectionInfo()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerLoginSuccessEvent event) {
        this.goMintCall(new GoMintBridgeProxyPlayerLoginSuccessEvent(event.getNetworkConnectionInfo()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerServerConnectRequestEvent event) {
        this.goMintCall(new GoMintBridgeProxyPlayerServerConnectRequestEvent(event.getNetworkConnectionInfo(), event.getNetworkServiceInfo()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerServerSwitchEvent event) {
        this.goMintCall(new GoMintBridgeProxyPlayerServerSwitchEvent(event.getNetworkConnectionInfo(), event.getNetworkServiceInfo()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerDisconnectEvent event) {
        this.goMintCall(new GoMintBridgeProxyPlayerDisconnectEvent(event.getNetworkConnectionInfo()));
    }

    @EventListener
    public void handle(BridgeServerPlayerLoginRequestEvent event) {
        this.goMintCall(new GoMintBridgeServerPlayerLoginRequestEvent(event.getNetworkConnectionInfo(), event.getNetworkPlayerServerInfo()));
    }

    @EventListener
    public void handle(BridgeServerPlayerLoginSuccessEvent event) {
        this.goMintCall(new GoMintBridgeServerPlayerLoginSuccessEvent(event.getNetworkConnectionInfo(), event.getNetworkPlayerServerInfo()));
    }

    @EventListener
    public void handle(BridgeServerPlayerDisconnectEvent event) {
        this.goMintCall(new GoMintBridgeServerPlayerDisconnectEvent(event.getNetworkConnectionInfo(), event.getNetworkPlayerServerInfo()));
    }

    private void goMintCall(Event event) {
        GoMint.instance().getPluginManager().callEvent(event);
    }

    private void goMintSyncExecution(Runnable runnable) {
        if (GoMint.instance().isMainThread()) {
            runnable.run();
            return;
        }

        this.plugin.getScheduler().execute(runnable);
    }

    private ITask<Void> listenableGoMintSyncExecution(Runnable runnable) {
        CompletableTask<Void> task = new CompletableTask<>();
        this.goMintSyncExecution(() -> {
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