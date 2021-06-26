/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dytanic.cloudnet.ext.bridge.sponge.listener;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.event.events.network.NetworkChannelPacketReceiveEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceConnectNetworkEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceDisconnectNetworkEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceInfoUpdateEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceRegisterEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceStartEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceStopEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceUnregisterEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeConfigurationUpdateEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeProxyPlayerDisconnectEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeProxyPlayerLoginRequestEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeProxyPlayerLoginSuccessEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeProxyPlayerServerConnectRequestEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeProxyPlayerServerSwitchEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeServerPlayerDisconnectEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeServerPlayerLoginRequestEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeServerPlayerLoginSuccessEvent;
import de.dytanic.cloudnet.ext.bridge.sponge.SpongeCloudNetHelper;
import de.dytanic.cloudnet.ext.bridge.sponge.event.SpongeBridgeConfigurationUpdateEvent;
import de.dytanic.cloudnet.ext.bridge.sponge.event.SpongeBridgeProxyPlayerDisconnectEvent;
import de.dytanic.cloudnet.ext.bridge.sponge.event.SpongeBridgeProxyPlayerLoginSuccessEvent;
import de.dytanic.cloudnet.ext.bridge.sponge.event.SpongeBridgeProxyPlayerServerConnectRequestEvent;
import de.dytanic.cloudnet.ext.bridge.sponge.event.SpongeBridgeProxyPlayerServerSwitchEvent;
import de.dytanic.cloudnet.ext.bridge.sponge.event.SpongeBridgeServerPlayerDisconnectEvent;
import de.dytanic.cloudnet.ext.bridge.sponge.event.SpongeBridgeServerPlayerLoginRequestEvent;
import de.dytanic.cloudnet.ext.bridge.sponge.event.SpongeBridgeServerPlayerLoginSuccessEvent;
import de.dytanic.cloudnet.ext.bridge.sponge.event.SpongeChannelMessageReceiveEvent;
import de.dytanic.cloudnet.ext.bridge.sponge.event.SpongeCloudServiceConnectNetworkEvent;
import de.dytanic.cloudnet.ext.bridge.sponge.event.SpongeCloudServiceDisconnectNetworkEvent;
import de.dytanic.cloudnet.ext.bridge.sponge.event.SpongeCloudServiceInfoUpdateEvent;
import de.dytanic.cloudnet.ext.bridge.sponge.event.SpongeCloudServiceRegisterEvent;
import de.dytanic.cloudnet.ext.bridge.sponge.event.SpongeCloudServiceStartEvent;
import de.dytanic.cloudnet.ext.bridge.sponge.event.SpongeCloudServiceStopEvent;
import de.dytanic.cloudnet.ext.bridge.sponge.event.SpongeCloudServiceUnregisterEvent;
import de.dytanic.cloudnet.ext.bridge.sponge.event.SpongeNetworkChannelPacketReceiveEvent;
import de.dytanic.cloudnet.ext.bridge.sponge.event.SpongeServiceInfoSnapshotConfigureEvent;
import de.dytanic.cloudnet.wrapper.event.service.ServiceInfoSnapshotConfigureEvent;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Event;

public final class SpongeCloudNetListener {

  @EventListener
  public void handle(ServiceInfoSnapshotConfigureEvent event) {
    SpongeCloudNetHelper.initProperties(event.getServiceInfoSnapshot());
    this.spongeCall(new SpongeServiceInfoSnapshotConfigureEvent(event.getServiceInfoSnapshot()));
  }

  @EventListener
  public void handle(CloudServiceInfoUpdateEvent event) {
    this.spongeCall(new SpongeCloudServiceInfoUpdateEvent(event.getServiceInfo()));
  }

  @EventListener
  public void handle(CloudServiceRegisterEvent event) {
    this.spongeCall(new SpongeCloudServiceRegisterEvent(event.getServiceInfo()));
  }

  @EventListener
  public void handle(CloudServiceStartEvent event) {
    this.spongeCall(new SpongeCloudServiceStartEvent(event.getServiceInfo()));
  }

  @EventListener
  public void handle(CloudServiceConnectNetworkEvent event) {
    this.spongeCall(new SpongeCloudServiceConnectNetworkEvent(event.getServiceInfo()));
  }

  @EventListener
  public void handle(CloudServiceDisconnectNetworkEvent event) {
    this.spongeCall(new SpongeCloudServiceDisconnectNetworkEvent(event.getServiceInfo()));
  }

  @EventListener
  public void handle(CloudServiceStopEvent event) {
    this.spongeCall(new SpongeCloudServiceStopEvent(event.getServiceInfo()));
  }

  @EventListener
  public void handle(CloudServiceUnregisterEvent event) {
    this.spongeCall(new SpongeCloudServiceUnregisterEvent(event.getServiceInfo()));
  }

  @EventListener
  public void handle(ChannelMessageReceiveEvent event) {
    this.spongeCall(new SpongeChannelMessageReceiveEvent(event));
  }

  @EventListener
  public void handle(NetworkChannelPacketReceiveEvent event) {
    this.spongeCall(new SpongeNetworkChannelPacketReceiveEvent(event.getChannel(), event.getPacket()));
  }

  @EventListener
  public void handle(BridgeConfigurationUpdateEvent event) {
    this.spongeCall(new SpongeBridgeConfigurationUpdateEvent(event.getBridgeConfiguration()));
  }

  @EventListener
  public void handle(BridgeProxyPlayerLoginRequestEvent event) {
    this.spongeCall(new SpongeBridgeProxyPlayerLoginSuccessEvent(event.getNetworkConnectionInfo()));
  }

  @EventListener
  public void handle(BridgeProxyPlayerLoginSuccessEvent event) {
    this.spongeCall(new SpongeBridgeProxyPlayerLoginSuccessEvent(event.getNetworkConnectionInfo()));
  }

  @EventListener
  public void handle(BridgeProxyPlayerServerConnectRequestEvent event) {
    this.spongeCall(new SpongeBridgeProxyPlayerServerConnectRequestEvent(event.getNetworkConnectionInfo(),
      event.getNetworkServiceInfo()));
  }

  @EventListener
  public void handle(BridgeProxyPlayerServerSwitchEvent event) {
    this.spongeCall(
      new SpongeBridgeProxyPlayerServerSwitchEvent(event.getNetworkConnectionInfo(), event.getNetworkServiceInfo()));
  }

  @EventListener
  public void handle(BridgeProxyPlayerDisconnectEvent event) {
    this.spongeCall(new SpongeBridgeProxyPlayerDisconnectEvent(event.getNetworkConnectionInfo()));
  }

  @EventListener
  public void handle(BridgeServerPlayerLoginRequestEvent event) {
    this.spongeCall(new SpongeBridgeServerPlayerLoginRequestEvent(event.getNetworkConnectionInfo(),
      event.getNetworkPlayerServerInfo()));
  }

  @EventListener
  public void handle(BridgeServerPlayerLoginSuccessEvent event) {
    this.spongeCall(new SpongeBridgeServerPlayerLoginSuccessEvent(event.getNetworkConnectionInfo(),
      event.getNetworkPlayerServerInfo()));
  }

  @EventListener
  public void handle(BridgeServerPlayerDisconnectEvent event) {
    this.spongeCall(new SpongeBridgeServerPlayerDisconnectEvent(event.getNetworkConnectionInfo(),
      event.getNetworkPlayerServerInfo()));
  }

  private void spongeCall(Event event) {
    Sponge.getEventManager().post(event);
  }
}
