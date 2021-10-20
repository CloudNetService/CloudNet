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

package de.dytanic.cloudnet.ext.bridge.bungee.listener;

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
import de.dytanic.cloudnet.driver.event.events.task.ServiceTaskAddEvent;
import de.dytanic.cloudnet.driver.event.events.task.ServiceTaskRemoveEvent;
import de.dytanic.cloudnet.ext.bridge.bungee.BungeeCloudNetHelper;
import de.dytanic.cloudnet.ext.bridge.bungee.event.BungeeBridgeConfigurationUpdateEvent;
import de.dytanic.cloudnet.ext.bridge.bungee.event.BungeeBridgeProxyPlayerDisconnectEvent;
import de.dytanic.cloudnet.ext.bridge.bungee.event.BungeeBridgeProxyPlayerLoginRequestEvent;
import de.dytanic.cloudnet.ext.bridge.bungee.event.BungeeBridgeProxyPlayerLoginSuccessEvent;
import de.dytanic.cloudnet.ext.bridge.bungee.event.BungeeBridgeProxyPlayerServerConnectRequestEvent;
import de.dytanic.cloudnet.ext.bridge.bungee.event.BungeeBridgeProxyPlayerServerSwitchEvent;
import de.dytanic.cloudnet.ext.bridge.bungee.event.BungeeBridgeServerPlayerDisconnectEvent;
import de.dytanic.cloudnet.ext.bridge.bungee.event.BungeeBridgeServerPlayerLoginRequestEvent;
import de.dytanic.cloudnet.ext.bridge.bungee.event.BungeeBridgeServerPlayerLoginSuccessEvent;
import de.dytanic.cloudnet.ext.bridge.bungee.event.BungeeChannelMessageReceiveEvent;
import de.dytanic.cloudnet.ext.bridge.bungee.event.BungeeCloudServiceConnectNetworkEvent;
import de.dytanic.cloudnet.ext.bridge.bungee.event.BungeeCloudServiceDisconnectNetworkEvent;
import de.dytanic.cloudnet.ext.bridge.bungee.event.BungeeCloudServiceInfoUpdateEvent;
import de.dytanic.cloudnet.ext.bridge.bungee.event.BungeeCloudServiceRegisterEvent;
import de.dytanic.cloudnet.ext.bridge.bungee.event.BungeeCloudServiceStartEvent;
import de.dytanic.cloudnet.ext.bridge.bungee.event.BungeeCloudServiceStopEvent;
import de.dytanic.cloudnet.ext.bridge.bungee.event.BungeeCloudServiceUnregisterEvent;
import de.dytanic.cloudnet.ext.bridge.bungee.event.BungeeNetworkChannelPacketReceiveEvent;
import de.dytanic.cloudnet.ext.bridge.bungee.event.BungeeServiceInfoSnapshotConfigureEvent;
import de.dytanic.cloudnet.ext.bridge.bungee.event.BungeeServiceTaskAddEvent;
import de.dytanic.cloudnet.ext.bridge.bungee.event.BungeeServiceTaskRemoveEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeConfigurationUpdateEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeProxyPlayerDisconnectEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeProxyPlayerLoginRequestEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeProxyPlayerLoginSuccessEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeProxyPlayerServerConnectRequestEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeProxyPlayerServerSwitchEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeServerPlayerDisconnectEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeServerPlayerLoginRequestEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeServerPlayerLoginSuccessEvent;
import de.dytanic.cloudnet.ext.bridge.proxy.BridgeProxyHelper;
import de.dytanic.cloudnet.wrapper.event.service.ServiceInfoSnapshotConfigureEvent;
import java.net.InetSocketAddress;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Event;

public final class BungeeCloudNetListener {

  @EventListener
  public void handle(ServiceInfoSnapshotConfigureEvent event) {
    BungeeCloudNetHelper.initProperties(event.getServiceInfoSnapshot());
    this.bungeeCall(new BungeeServiceInfoSnapshotConfigureEvent(event.getServiceInfoSnapshot()));
  }

  @EventListener
  public void handle(CloudServiceStartEvent event) {
    if (BungeeCloudNetHelper.isServiceEnvironmentTypeProvidedForBungeeCord(event.getServiceInfo())) {
      if (event.getServiceInfo().getProperties().contains("Online-Mode") && event.getServiceInfo().getProperties()
        .getBoolean("Online-Mode")) {
        return;
      }

      String name = event.getServiceInfo().getServiceId().getName();

      ProxyServer.getInstance().getServers()
        .put(name, BungeeCloudNetHelper.createServerInfo(name, new InetSocketAddress(
          event.getServiceInfo().getConnectAddress().getHost(),
          event.getServiceInfo().getConnectAddress().getPort()
        )));
    }

    this.bungeeCall(new BungeeCloudServiceStartEvent(event.getServiceInfo()));
  }

  @EventListener
  public void handle(CloudServiceStopEvent event) {
    if (BungeeCloudNetHelper.isServiceEnvironmentTypeProvidedForBungeeCord(event.getServiceInfo())) {
      ProxyServer.getInstance().getServers().remove(event.getServiceInfo().getName());
      BridgeProxyHelper.cacheServiceInfoSnapshot(event.getServiceInfo());
    }

    this.bungeeCall(new BungeeCloudServiceStopEvent(event.getServiceInfo()));
  }

  @EventListener
  public void handle(CloudServiceInfoUpdateEvent event) {
    if (BungeeCloudNetHelper.isServiceEnvironmentTypeProvidedForBungeeCord(event.getServiceInfo())) {
      BridgeProxyHelper.cacheServiceInfoSnapshot(event.getServiceInfo());
    }

    this.bungeeCall(new BungeeCloudServiceInfoUpdateEvent(event.getServiceInfo()));
  }

  @EventListener
  public void handle(CloudServiceRegisterEvent event) {
    if (BungeeCloudNetHelper.isServiceEnvironmentTypeProvidedForBungeeCord(event.getServiceInfo())) {
      BridgeProxyHelper.cacheServiceInfoSnapshot(event.getServiceInfo());
    }

    this.bungeeCall(new BungeeCloudServiceRegisterEvent(event.getServiceInfo()));
  }

  @EventListener
  public void handle(CloudServiceConnectNetworkEvent event) {
    if (BungeeCloudNetHelper.isServiceEnvironmentTypeProvidedForBungeeCord(event.getServiceInfo())) {
      BridgeProxyHelper.cacheServiceInfoSnapshot(event.getServiceInfo());
    }

    this.bungeeCall(new BungeeCloudServiceConnectNetworkEvent(event.getServiceInfo()));
  }

  @EventListener
  public void handle(CloudServiceDisconnectNetworkEvent event) {
    if (BungeeCloudNetHelper.isServiceEnvironmentTypeProvidedForBungeeCord(event.getServiceInfo())) {
      BridgeProxyHelper.cacheServiceInfoSnapshot(event.getServiceInfo());
    }

    this.bungeeCall(new BungeeCloudServiceDisconnectNetworkEvent(event.getServiceInfo()));
  }

  @EventListener
  public void handle(CloudServiceUnregisterEvent event) {
    if (BungeeCloudNetHelper.isServiceEnvironmentTypeProvidedForBungeeCord(event.getServiceInfo())) {
      BridgeProxyHelper.removeCachedServiceInfoSnapshot(event.getServiceInfo());
    }

    this.bungeeCall(new BungeeCloudServiceUnregisterEvent(event.getServiceInfo()));
  }

  @EventListener
  public void handle(ServiceTaskAddEvent event) {
    this.bungeeCall(new BungeeServiceTaskAddEvent(event.getTask()));
  }

  @EventListener
  public void handle(ServiceTaskRemoveEvent event) {
    this.bungeeCall(new BungeeServiceTaskRemoveEvent(event.getTask()));
  }

  @EventListener
  public void handle(ChannelMessageReceiveEvent event) {
    this.bungeeCall(new BungeeChannelMessageReceiveEvent(event));
  }

  @EventListener
  public void handle(NetworkChannelPacketReceiveEvent event) {
    this.bungeeCall(new BungeeNetworkChannelPacketReceiveEvent(event.getChannel(), event.getPacket()));
  }

  @EventListener
  public void handle(BridgeConfigurationUpdateEvent event) {
    this.bungeeCall(new BungeeBridgeConfigurationUpdateEvent(event.getBridgeConfiguration()));
  }

  @EventListener
  public void handle(BridgeProxyPlayerLoginRequestEvent event) {
    this.bungeeCall(new BungeeBridgeProxyPlayerLoginRequestEvent(event.getNetworkConnectionInfo()));
  }

  @EventListener
  public void handle(BridgeProxyPlayerLoginSuccessEvent event) {
    this.bungeeCall(new BungeeBridgeProxyPlayerLoginSuccessEvent(event.getNetworkConnectionInfo()));
  }

  @EventListener
  public void handle(BridgeProxyPlayerServerConnectRequestEvent event) {
    this.bungeeCall(new BungeeBridgeProxyPlayerServerConnectRequestEvent(event.getNetworkConnectionInfo(),
      event.getNetworkServiceInfo()));
  }

  @EventListener
  public void handle(BridgeProxyPlayerServerSwitchEvent event) {
    this.bungeeCall(
      new BungeeBridgeProxyPlayerServerSwitchEvent(event.getNetworkConnectionInfo(), event.getNetworkServiceInfo()));
  }

  @EventListener
  public void handle(BridgeProxyPlayerDisconnectEvent event) {
    this.bungeeCall(new BungeeBridgeProxyPlayerDisconnectEvent(event.getNetworkConnectionInfo()));
  }

  @EventListener
  public void handle(BridgeServerPlayerLoginRequestEvent event) {
    this.bungeeCall(new BungeeBridgeServerPlayerLoginRequestEvent(event.getNetworkConnectionInfo(),
      event.getNetworkPlayerServerInfo()));
  }

  @EventListener
  public void handle(BridgeServerPlayerLoginSuccessEvent event) {
    this.bungeeCall(new BungeeBridgeServerPlayerLoginSuccessEvent(event.getNetworkConnectionInfo(),
      event.getNetworkPlayerServerInfo()));
  }

  @EventListener
  public void handle(BridgeServerPlayerDisconnectEvent event) {
    this.bungeeCall(new BungeeBridgeServerPlayerDisconnectEvent(event.getNetworkConnectionInfo(),
      event.getNetworkPlayerServerInfo()));
  }

  @Deprecated
  private void bungeeCall(Event event) {
    ProxyServer.getInstance().getPluginManager().callEvent(event);
  }

}
