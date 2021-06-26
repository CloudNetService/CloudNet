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

package de.dytanic.cloudnet.ext.bridge.bukkit.listener;

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
import de.dytanic.cloudnet.ext.bridge.bukkit.BukkitCloudNetHelper;
import de.dytanic.cloudnet.ext.bridge.bukkit.event.BukkitBridgeConfigurationUpdateEvent;
import de.dytanic.cloudnet.ext.bridge.bukkit.event.BukkitBridgeProxyPlayerDisconnectEvent;
import de.dytanic.cloudnet.ext.bridge.bukkit.event.BukkitBridgeProxyPlayerLoginSuccessEvent;
import de.dytanic.cloudnet.ext.bridge.bukkit.event.BukkitBridgeProxyPlayerServerConnectRequestEvent;
import de.dytanic.cloudnet.ext.bridge.bukkit.event.BukkitBridgeProxyPlayerServerSwitchEvent;
import de.dytanic.cloudnet.ext.bridge.bukkit.event.BukkitBridgeServerPlayerDisconnectEvent;
import de.dytanic.cloudnet.ext.bridge.bukkit.event.BukkitBridgeServerPlayerLoginRequestEvent;
import de.dytanic.cloudnet.ext.bridge.bukkit.event.BukkitBridgeServerPlayerLoginSuccessEvent;
import de.dytanic.cloudnet.ext.bridge.bukkit.event.BukkitChannelMessageReceiveEvent;
import de.dytanic.cloudnet.ext.bridge.bukkit.event.BukkitCloudServiceConnectNetworkEvent;
import de.dytanic.cloudnet.ext.bridge.bukkit.event.BukkitCloudServiceDisconnectNetworkEvent;
import de.dytanic.cloudnet.ext.bridge.bukkit.event.BukkitCloudServiceInfoUpdateEvent;
import de.dytanic.cloudnet.ext.bridge.bukkit.event.BukkitCloudServiceRegisterEvent;
import de.dytanic.cloudnet.ext.bridge.bukkit.event.BukkitCloudServiceStartEvent;
import de.dytanic.cloudnet.ext.bridge.bukkit.event.BukkitCloudServiceStopEvent;
import de.dytanic.cloudnet.ext.bridge.bukkit.event.BukkitCloudServiceUnregisterEvent;
import de.dytanic.cloudnet.ext.bridge.bukkit.event.BukkitNetworkChannelPacketReceiveEvent;
import de.dytanic.cloudnet.ext.bridge.bukkit.event.BukkitServiceInfoSnapshotConfigureEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeConfigurationUpdateEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeProxyPlayerDisconnectEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeProxyPlayerLoginRequestEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeProxyPlayerLoginSuccessEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeProxyPlayerServerConnectRequestEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeProxyPlayerServerSwitchEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeServerPlayerDisconnectEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeServerPlayerLoginRequestEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeServerPlayerLoginSuccessEvent;
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
    this.bukkitCall(new BukkitChannelMessageReceiveEvent(event));
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
    this.bukkitCall(new BukkitBridgeProxyPlayerServerConnectRequestEvent(event.getNetworkConnectionInfo(),
      event.getNetworkServiceInfo()));
  }

  @EventListener
  public void handle(BridgeProxyPlayerServerSwitchEvent event) {
    this.bukkitCall(
      new BukkitBridgeProxyPlayerServerSwitchEvent(event.getNetworkConnectionInfo(), event.getNetworkServiceInfo()));
  }

  @EventListener
  public void handle(BridgeProxyPlayerDisconnectEvent event) {
    this.bukkitCall(new BukkitBridgeProxyPlayerDisconnectEvent(event.getNetworkConnectionInfo()));
  }

  @EventListener
  public void handle(BridgeServerPlayerLoginRequestEvent event) {
    this.bukkitCall(new BukkitBridgeServerPlayerLoginRequestEvent(event.getNetworkConnectionInfo(),
      event.getNetworkPlayerServerInfo()));
  }

  @EventListener
  public void handle(BridgeServerPlayerLoginSuccessEvent event) {
    this.bukkitCall(new BukkitBridgeServerPlayerLoginSuccessEvent(event.getNetworkConnectionInfo(),
      event.getNetworkPlayerServerInfo()));
  }

  @EventListener
  public void handle(BridgeServerPlayerDisconnectEvent event) {
    this.bukkitCall(new BukkitBridgeServerPlayerDisconnectEvent(event.getNetworkConnectionInfo(),
      event.getNetworkPlayerServerInfo()));
  }

  private void bukkitCall(Event event) {
    Bukkit.getPluginManager().callEvent(event);
  }

}
