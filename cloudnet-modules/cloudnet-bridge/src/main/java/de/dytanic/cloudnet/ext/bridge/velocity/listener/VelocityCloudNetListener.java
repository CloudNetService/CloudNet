package de.dytanic.cloudnet.ext.bridge.velocity.listener;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.event.events.network.NetworkChannelPacketReceiveEvent;
import de.dytanic.cloudnet.driver.event.events.service.*;
import de.dytanic.cloudnet.ext.bridge.event.*;
import de.dytanic.cloudnet.ext.bridge.proxy.BridgeProxyHelper;
import de.dytanic.cloudnet.ext.bridge.velocity.VelocityCloudNetHelper;
import de.dytanic.cloudnet.ext.bridge.velocity.event.*;
import de.dytanic.cloudnet.wrapper.event.service.ServiceInfoSnapshotConfigureEvent;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public final class VelocityCloudNetListener {

    @EventListener
    public void handle(ServiceInfoSnapshotConfigureEvent event) throws ExecutionException, InterruptedException {
        VelocityCloudNetHelper.initProperties(event.getServiceInfoSnapshot());
        this.velocityCall(new VelocityServiceInfoSnapshotConfigureEvent(event.getServiceInfoSnapshot())).get();
    }

    @EventListener
    public void handle(CloudServiceStartEvent event) {
        if (VelocityCloudNetHelper.isServiceEnvironmentTypeProvidedForVelocity(event.getServiceInfo())) {
            if (event.getServiceInfo().getProperties().contains("Online-Mode") && event.getServiceInfo().getProperties().getBoolean("Online-Mode")) {
                return;
            }

            String name = event.getServiceInfo().getServiceId().getName();
            VelocityCloudNetHelper.getProxyServer().registerServer(new ServerInfo(name, new InetSocketAddress(
                    event.getServiceInfo().getConnectAddress().getHost(),
                    event.getServiceInfo().getConnectAddress().getPort()
            )));

            VelocityCloudNetHelper.addServerToVelocityPrioritySystemConfiguration(event.getServiceInfo(), name);
        }

        this.velocityCall(new VelocityCloudServiceStartEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceStopEvent event) {
        if (VelocityCloudNetHelper.isServiceEnvironmentTypeProvidedForVelocity(event.getServiceInfo())) {
            String name = event.getServiceInfo().getServiceId().getName();

            VelocityCloudNetHelper.getProxyServer().getServer(name)
                    .map(RegisteredServer::getServerInfo)
                    .ifPresent(VelocityCloudNetHelper.getProxyServer()::unregisterServer);

            VelocityCloudNetHelper.removeServerToVelocityPrioritySystemConfiguration(event.getServiceInfo(), name);
            BridgeProxyHelper.cacheServiceInfoSnapshot(event.getServiceInfo());
        }

        this.velocityCall(new VelocityCloudServiceStopEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceInfoUpdateEvent event) {
        if (VelocityCloudNetHelper.isServiceEnvironmentTypeProvidedForVelocity(event.getServiceInfo())) {
            BridgeProxyHelper.cacheServiceInfoSnapshot(event.getServiceInfo());
        }

        this.velocityCall(new VelocityCloudServiceInfoUpdateEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceRegisterEvent event) {
        if (VelocityCloudNetHelper.isServiceEnvironmentTypeProvidedForVelocity(event.getServiceInfo())) {
            BridgeProxyHelper.cacheServiceInfoSnapshot(event.getServiceInfo());
        }

        this.velocityCall(new VelocityCloudServiceRegisterEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceConnectNetworkEvent event) {
        if (VelocityCloudNetHelper.isServiceEnvironmentTypeProvidedForVelocity(event.getServiceInfo())) {
            BridgeProxyHelper.cacheServiceInfoSnapshot(event.getServiceInfo());
        }

        this.velocityCall(new VelocityCloudServiceConnectNetworkEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceDisconnectNetworkEvent event) {
        if (VelocityCloudNetHelper.isServiceEnvironmentTypeProvidedForVelocity(event.getServiceInfo())) {
            BridgeProxyHelper.cacheServiceInfoSnapshot(event.getServiceInfo());
        }

        this.velocityCall(new VelocityCloudServiceDisconnectNetworkEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceUnregisterEvent event) {
        if (VelocityCloudNetHelper.isServiceEnvironmentTypeProvidedForVelocity(event.getServiceInfo())) {
            BridgeProxyHelper.removeCachedServiceInfoSnapshot(event.getServiceInfo());
        }

        this.velocityCall(new VelocityCloudServiceUnregisterEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(ChannelMessageReceiveEvent event) throws ExecutionException, InterruptedException {
        this.velocityCall(new VelocityChannelMessageReceiveEvent(event)).get();
    }

    @EventListener
    public void handle(NetworkChannelPacketReceiveEvent event) throws ExecutionException, InterruptedException {
        this.velocityCall(new VelocityNetworkChannelPacketReceiveEvent(event.getChannel(), event.getPacket())).get();
    }

    @EventListener
    public void handle(BridgeConfigurationUpdateEvent event) {
        this.velocityCall(new VelocityBridgeConfigurationUpdateEvent(event.getBridgeConfiguration()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerLoginRequestEvent event) {
        this.velocityCall(new VelocityBridgeProxyPlayerLoginSuccessEvent(event.getNetworkConnectionInfo()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerLoginSuccessEvent event) {
        this.velocityCall(new VelocityBridgeProxyPlayerLoginSuccessEvent(event.getNetworkConnectionInfo()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerServerConnectRequestEvent event) {
        this.velocityCall(new VelocityBridgeProxyPlayerServerConnectRequestEvent(event.getNetworkConnectionInfo(), event.getNetworkServiceInfo()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerServerSwitchEvent event) {
        this.velocityCall(new VelocityBridgeProxyPlayerServerSwitchEvent(event.getNetworkConnectionInfo(), event.getNetworkServiceInfo()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerDisconnectEvent event) {
        this.velocityCall(new VelocityBridgeProxyPlayerDisconnectEvent(event.getNetworkConnectionInfo()));
    }

    @EventListener
    public void handle(BridgeServerPlayerLoginRequestEvent event) {
        this.velocityCall(new VelocityBridgeServerPlayerLoginRequestEvent(event.getNetworkConnectionInfo(), event.getNetworkPlayerServerInfo()));
    }

    @EventListener
    public void handle(BridgeServerPlayerLoginSuccessEvent event) {
        this.velocityCall(new VelocityBridgeServerPlayerLoginSuccessEvent(event.getNetworkConnectionInfo(), event.getNetworkPlayerServerInfo()));
    }

    @EventListener
    public void handle(BridgeServerPlayerDisconnectEvent event) {
        this.velocityCall(new VelocityBridgeServerPlayerDisconnectEvent(event.getNetworkConnectionInfo(), event.getNetworkPlayerServerInfo()));
    }

    private <E> CompletableFuture<E> velocityCall(E event) {
        return VelocityCloudNetHelper.getProxyServer().getEventManager().fire(event);
    }

}