package de.dytanic.cloudnet.ext.bridge.sponge.listener;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.event.events.instance.CloudNetTickEvent;
import de.dytanic.cloudnet.driver.event.events.network.NetworkChannelPacketReceiveEvent;
import de.dytanic.cloudnet.driver.event.events.network.NetworkClusterNodeInfoUpdateEvent;
import de.dytanic.cloudnet.ext.bridge.event.*;
import de.dytanic.cloudnet.ext.bridge.sponge.SpongeCloudNetHelper;
import de.dytanic.cloudnet.ext.bridge.sponge.event.*;
import de.dytanic.cloudnet.wrapper.event.service.ServiceInfoSnapshotConfigureEvent;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Event;

public final class SpongeCloudNetListener {

  @EventListener
  public void handle(ServiceInfoSnapshotConfigureEvent event) {
    SpongeCloudNetHelper.initProperties(event.getServiceInfoSnapshot());
    this.spongeCall(new SpongeServiceInfoSnapshotConfigureEvent(
      event.getServiceInfoSnapshot()));
  }

  @EventListener
  public void handle(CloudServiceInfoUpdateEvent event) {
    this.spongeCall(
      new SpongeCloudServiceInfoUpdateEvent(event.getServiceInfo()));
  }

  @EventListener
  public void handle(CloudServiceRegisterEvent event) {
    this.spongeCall(
      new SpongeCloudServiceRegisterEvent(event.getServiceInfo()));
  }

  @EventListener
  public void handle(CloudServiceStartEvent event) {
    this.spongeCall(new SpongeCloudServiceStartEvent(event.getServiceInfo()));
  }

  @EventListener
  public void handle(CloudServiceConnectNetworkEvent event) {
    this.spongeCall(
      new SpongeCloudServiceConnectNetworkEvent(event.getServiceInfo()));
  }

  @EventListener
  public void handle(CloudServiceDisconnectNetworkEvent event) {
    this.spongeCall(
      new SpongeCloudServiceDisconnectNetworkEvent(event.getServiceInfo()));
  }

  @EventListener
  public void handle(CloudServiceStopEvent event) {
    this.spongeCall(new SpongeCloudServiceStopEvent(event.getServiceInfo()));
  }

  @EventListener
  public void handle(CloudServiceUnregisterEvent event) {
    this.spongeCall(
      new SpongeCloudServiceUnregisterEvent(event.getServiceInfo()));
  }

  @EventListener
  public void handle(ChannelMessageReceiveEvent event) {
    this.spongeCall(new SpongeChannelMessageReceiveEvent(event.getChannel(),
      event.getMessage(), event.getData()));
  }

  @EventListener
  public void handle(CloudNetTickEvent event) {
    this.spongeCall(new SpongeCloudNetTickEvent());
  }

  @EventListener
  public void handle(NetworkClusterNodeInfoUpdateEvent event) {
    this.spongeCall(new SpongeNetworkClusterNodeInfoUpdateEvent(
      event.getNetworkClusterNodeInfoSnapshot()));
  }

  @EventListener
  public void handle(NetworkChannelPacketReceiveEvent event) {
    this.spongeCall(
      new SpongeNetworkChannelPacketReceiveEvent(event.getChannel(),
        event.getPacket()));
  }

  @EventListener
  public void handle(BridgeConfigurationUpdateEvent event) {
    this.spongeCall(new SpongeBridgeConfigurationUpdateEvent(
      event.getBridgeConfiguration()));
  }

  @EventListener
  public void handle(BridgeProxyPlayerLoginRequestEvent event) {
    this.spongeCall(new SpongeBridgeProxyPlayerLoginSuccessEvent(
      event.getNetworkConnectionInfo()));
  }

  @EventListener
  public void handle(BridgeProxyPlayerLoginSuccessEvent event) {
    this.spongeCall(new SpongeBridgeProxyPlayerLoginSuccessEvent(
      event.getNetworkConnectionInfo()));
  }

  @EventListener
  public void handle(BridgeProxyPlayerServerConnectRequestEvent event) {
    this.spongeCall(new SpongeBridgeProxyPlayerServerConnectRequestEvent(
      event.getNetworkConnectionInfo(), event.getNetworkServiceInfo()));
  }

  @EventListener
  public void handle(BridgeProxyPlayerServerSwitchEvent event) {
    this.spongeCall(new SpongeBridgeProxyPlayerServerSwitchEvent(
      event.getNetworkConnectionInfo(), event.getNetworkServiceInfo()));
  }

  @EventListener
  public void handle(BridgeProxyPlayerDisconnectEvent event) {
    this.spongeCall(new SpongeBridgeProxyPlayerDisconnectEvent(
      event.getNetworkConnectionInfo()));
  }

  @EventListener
  public void handle(BridgeServerPlayerLoginRequestEvent event) {
    this.spongeCall(new SpongeBridgeServerPlayerLoginRequestEvent(
      event.getNetworkConnectionInfo(), event.getNetworkPlayerServerInfo()));
  }

  @EventListener
  public void handle(BridgeServerPlayerLoginSuccessEvent event) {
    this.spongeCall(new SpongeBridgeServerPlayerLoginSuccessEvent(
      event.getNetworkConnectionInfo(), event.getNetworkPlayerServerInfo()));
  }

  @EventListener
  public void handle(BridgeServerPlayerDisconnectEvent event) {
    this.spongeCall(new SpongeBridgeServerPlayerDisconnectEvent(
      event.getNetworkConnectionInfo(), event.getNetworkPlayerServerInfo()));
  }

  private void spongeCall(Event event) {
    Sponge.getEventManager().post(event);
  }
}