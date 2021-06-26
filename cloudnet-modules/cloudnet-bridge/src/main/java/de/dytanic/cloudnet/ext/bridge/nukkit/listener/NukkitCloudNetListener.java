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

package de.dytanic.cloudnet.ext.bridge.nukkit.listener;

import cn.nukkit.Server;
import cn.nukkit.event.Event;
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
import de.dytanic.cloudnet.ext.bridge.nukkit.NukkitCloudNetHelper;
import de.dytanic.cloudnet.ext.bridge.nukkit.event.NukkitBridgeConfigurationUpdateEvent;
import de.dytanic.cloudnet.ext.bridge.nukkit.event.NukkitBridgeProxyPlayerDisconnectEvent;
import de.dytanic.cloudnet.ext.bridge.nukkit.event.NukkitBridgeProxyPlayerLoginSuccessEvent;
import de.dytanic.cloudnet.ext.bridge.nukkit.event.NukkitBridgeProxyPlayerServerConnectRequestEvent;
import de.dytanic.cloudnet.ext.bridge.nukkit.event.NukkitBridgeProxyPlayerServerSwitchEvent;
import de.dytanic.cloudnet.ext.bridge.nukkit.event.NukkitBridgeServerPlayerDisconnectEvent;
import de.dytanic.cloudnet.ext.bridge.nukkit.event.NukkitBridgeServerPlayerLoginRequestEvent;
import de.dytanic.cloudnet.ext.bridge.nukkit.event.NukkitBridgeServerPlayerLoginSuccessEvent;
import de.dytanic.cloudnet.ext.bridge.nukkit.event.NukkitChannelMessageReceiveEvent;
import de.dytanic.cloudnet.ext.bridge.nukkit.event.NukkitCloudServiceConnectNetworkEvent;
import de.dytanic.cloudnet.ext.bridge.nukkit.event.NukkitCloudServiceDisconnectNetworkEvent;
import de.dytanic.cloudnet.ext.bridge.nukkit.event.NukkitCloudServiceInfoUpdateEvent;
import de.dytanic.cloudnet.ext.bridge.nukkit.event.NukkitCloudServiceRegisterEvent;
import de.dytanic.cloudnet.ext.bridge.nukkit.event.NukkitCloudServiceStartEvent;
import de.dytanic.cloudnet.ext.bridge.nukkit.event.NukkitCloudServiceStopEvent;
import de.dytanic.cloudnet.ext.bridge.nukkit.event.NukkitCloudServiceUnregisterEvent;
import de.dytanic.cloudnet.ext.bridge.nukkit.event.NukkitNetworkChannelPacketReceiveEvent;
import de.dytanic.cloudnet.ext.bridge.nukkit.event.NukkitServiceInfoSnapshotConfigureEvent;
import de.dytanic.cloudnet.wrapper.event.service.ServiceInfoSnapshotConfigureEvent;

public final class NukkitCloudNetListener {

  @EventListener
  public void handle(ServiceInfoSnapshotConfigureEvent event) {
    NukkitCloudNetHelper.initProperties(event.getServiceInfoSnapshot());
    this.nukkitCall(new NukkitServiceInfoSnapshotConfigureEvent(event.getServiceInfoSnapshot()));
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
  public void handle(ChannelMessageReceiveEvent event) {
    this.nukkitCall(new NukkitChannelMessageReceiveEvent(event));
  }

  @EventListener
  public void handle(NetworkChannelPacketReceiveEvent event) {
    this.nukkitCall(new NukkitNetworkChannelPacketReceiveEvent(event.getChannel(), event.getPacket()));
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
    this.nukkitCall(new NukkitBridgeProxyPlayerServerConnectRequestEvent(event.getNetworkConnectionInfo(),
      event.getNetworkServiceInfo()));
  }

  @EventListener
  public void handle(BridgeProxyPlayerServerSwitchEvent event) {
    this.nukkitCall(
      new NukkitBridgeProxyPlayerServerSwitchEvent(event.getNetworkConnectionInfo(), event.getNetworkServiceInfo()));
  }

  @EventListener
  public void handle(BridgeProxyPlayerDisconnectEvent event) {
    this.nukkitCall(new NukkitBridgeProxyPlayerDisconnectEvent(event.getNetworkConnectionInfo()));
  }

  @EventListener
  public void handle(BridgeServerPlayerLoginRequestEvent event) {
    this.nukkitCall(new NukkitBridgeServerPlayerLoginRequestEvent(event.getNetworkConnectionInfo(),
      event.getNetworkPlayerServerInfo()));
  }

  @EventListener
  public void handle(BridgeServerPlayerLoginSuccessEvent event) {
    this.nukkitCall(new NukkitBridgeServerPlayerLoginSuccessEvent(event.getNetworkConnectionInfo(),
      event.getNetworkPlayerServerInfo()));
  }

  @EventListener
  public void handle(BridgeServerPlayerDisconnectEvent event) {
    this.nukkitCall(new NukkitBridgeServerPlayerDisconnectEvent(event.getNetworkConnectionInfo(),
      event.getNetworkPlayerServerInfo()));
  }

  private void nukkitCall(Event event) {
    Server.getInstance().getPluginManager().callEvent(event);
  }

}
