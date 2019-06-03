package de.dytanic.cloudnet.ext.bridge.proxprox.listener;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.event.events.instance.CloudNetTickEvent;
import de.dytanic.cloudnet.driver.event.events.network.NetworkChannelPacketReceiveEvent;
import de.dytanic.cloudnet.driver.event.events.network.NetworkClusterNodeInfoUpdateEvent;
import de.dytanic.cloudnet.driver.event.events.service.*;
import de.dytanic.cloudnet.ext.bridge.event.*;
import de.dytanic.cloudnet.ext.bridge.proxprox.ProxProxCloudNetHelper;
import de.dytanic.cloudnet.ext.bridge.proxprox.event.*;
import de.dytanic.cloudnet.wrapper.event.service.ServiceInfoSnapshotConfigureEvent;
import io.gomint.proxprox.api.plugin.event.Event;

public final class ProxProxCloudNetListener {

    @EventListener
    public void handle(ServiceInfoSnapshotConfigureEvent event)
    {
        ProxProxCloudNetHelper.initProperties(event.getServiceInfoSnapshot());
        this.proxproxCall(new ProxProxServiceInfoSnapshotConfigureEvent(event.getServiceInfoSnapshot()));
    }

    @EventListener
    public void handle(CloudServiceInfoUpdateEvent event)
    {
        if (ProxProxCloudNetHelper.isServiceEnvironmentTypeProvidedForProxProx(event.getServiceInfo()))
            ProxProxCloudNetHelper.SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION.put(event.getServiceInfo().getServiceId().getName(), event.getServiceInfo());

        this.proxproxCall(new ProxProxCloudServiceInfoUpdateEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceRegisterEvent event)
    {
        if (ProxProxCloudNetHelper.isServiceEnvironmentTypeProvidedForProxProx(event.getServiceInfo()))
            ProxProxCloudNetHelper.SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION.put(event.getServiceInfo().getServiceId().getName(), event.getServiceInfo());

        this.proxproxCall(new ProxProxCloudServiceRegisterEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceStartEvent event)
    {
        this.proxproxCall(new ProxProxCloudServiceStartEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceConnectNetworkEvent event)
    {
        if (ProxProxCloudNetHelper.isServiceEnvironmentTypeProvidedForProxProx(event.getServiceInfo()))
            ProxProxCloudNetHelper.SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION.put(event.getServiceInfo().getServiceId().getName(), event.getServiceInfo());

        this.proxproxCall(new ProxProxCloudServiceConnectNetworkEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceDisconnectNetworkEvent event)
    {
        if (ProxProxCloudNetHelper.isServiceEnvironmentTypeProvidedForProxProx(event.getServiceInfo()))
            ProxProxCloudNetHelper.SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION.put(event.getServiceInfo().getServiceId().getName(), event.getServiceInfo());

        this.proxproxCall(new ProxProxCloudServiceDisconnectNetworkEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceStopEvent event)
    {
        if (ProxProxCloudNetHelper.isServiceEnvironmentTypeProvidedForProxProx(event.getServiceInfo()))
        {
            String name = event.getServiceInfo().getServiceId().getName();
            ProxProxCloudNetHelper.SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION.put(name, event.getServiceInfo());
        }

        this.proxproxCall(new ProxProxCloudServiceStopEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceUnregisterEvent event)
    {
        if (ProxProxCloudNetHelper.isServiceEnvironmentTypeProvidedForProxProx(event.getServiceInfo()))
            ProxProxCloudNetHelper.SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION.remove(event.getServiceInfo().getServiceId().getName());

        this.proxproxCall(new ProxProxCloudServiceUnregisterEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(ChannelMessageReceiveEvent event)
    {
        this.proxproxCall(new ProxProxChannelMessageReceiveEvent(event.getChannel(), event.getMessage(), event.getData()));
    }

    @EventListener
    public void handle(CloudNetTickEvent event)
    {
        this.proxproxCall(new ProxProxCloudNetTickEvent());
    }

    @EventListener
    public void handle(NetworkClusterNodeInfoUpdateEvent event)
    {
        this.proxproxCall(new ProxProxNetworkClusterNodeInfoUpdateEvent(event.getNetworkClusterNodeInfoSnapshot()));
    }

    @EventListener
    public void handle(NetworkChannelPacketReceiveEvent event)
    {
        this.proxproxCall(new ProxProxNetworkChannelPacketReceiveEvent(event.getChannel(), event.getPacket()));
    }

    @EventListener
    public void handle(BridgeConfigurationUpdateEvent event)
    {
        this.proxproxCall(new ProxProxBridgeConfigurationUpdateEvent(event.getBridgeConfiguration()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerLoginRequestEvent event)
    {
        this.proxproxCall(new ProxProxBridgeProxyPlayerLoginSuccessEvent(event.getNetworkConnectionInfo()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerLoginSuccessEvent event)
    {
        this.proxproxCall(new ProxProxBridgeProxyPlayerLoginSuccessEvent(event.getNetworkConnectionInfo()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerServerConnectRequestEvent event)
    {
        this.proxproxCall(new ProxProxBridgeProxyPlayerServerConnectRequestEvent(event.getNetworkConnectionInfo(), event.getNetworkServiceInfo()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerServerSwitchEvent event)
    {
        this.proxproxCall(new ProxProxBridgeProxyPlayerServerSwitchEvent(event.getNetworkConnectionInfo(), event.getNetworkServiceInfo()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerDisconnectEvent event)
    {
        this.proxproxCall(new ProxProxBridgeProxyPlayerDisconnectEvent(event.getNetworkConnectionInfo()));
    }

    @EventListener
    public void handle(BridgeServerPlayerLoginRequestEvent event)
    {
        this.proxproxCall(new ProxProxBridgeServerPlayerLoginRequestEvent(event.getNetworkConnectionInfo(), event.getNetworkPlayerServerInfo()));
    }

    @EventListener
    public void handle(BridgeServerPlayerLoginSuccessEvent event)
    {
        this.proxproxCall(new ProxProxBridgeServerPlayerLoginSuccessEvent(event.getNetworkConnectionInfo(), event.getNetworkPlayerServerInfo()));
    }

    @EventListener
    public void handle(BridgeServerPlayerDisconnectEvent event)
    {
        this.proxproxCall(new ProxProxBridgeServerPlayerDisconnectEvent(event.getNetworkConnectionInfo(), event.getNetworkPlayerServerInfo()));
    }

    private void proxproxCall(Event event)
    {
        ProxProxCloudNetHelper.getProxyServer().getPluginManager().callEvent(event);
    }
}