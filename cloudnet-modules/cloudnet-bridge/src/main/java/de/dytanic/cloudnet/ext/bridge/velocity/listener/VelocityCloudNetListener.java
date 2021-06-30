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

package de.dytanic.cloudnet.ext.bridge.velocity.listener;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
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
import de.dytanic.cloudnet.ext.bridge.proxy.BridgeProxyHelper;
import de.dytanic.cloudnet.ext.bridge.velocity.VelocityCloudNetHelper;
import de.dytanic.cloudnet.ext.bridge.velocity.event.VelocityBridgeConfigurationUpdateEvent;
import de.dytanic.cloudnet.ext.bridge.velocity.event.VelocityBridgeProxyPlayerDisconnectEvent;
import de.dytanic.cloudnet.ext.bridge.velocity.event.VelocityBridgeProxyPlayerLoginSuccessEvent;
import de.dytanic.cloudnet.ext.bridge.velocity.event.VelocityBridgeProxyPlayerServerConnectRequestEvent;
import de.dytanic.cloudnet.ext.bridge.velocity.event.VelocityBridgeProxyPlayerServerSwitchEvent;
import de.dytanic.cloudnet.ext.bridge.velocity.event.VelocityBridgeServerPlayerDisconnectEvent;
import de.dytanic.cloudnet.ext.bridge.velocity.event.VelocityBridgeServerPlayerLoginRequestEvent;
import de.dytanic.cloudnet.ext.bridge.velocity.event.VelocityBridgeServerPlayerLoginSuccessEvent;
import de.dytanic.cloudnet.ext.bridge.velocity.event.VelocityChannelMessageReceiveEvent;
import de.dytanic.cloudnet.ext.bridge.velocity.event.VelocityCloudServiceConnectNetworkEvent;
import de.dytanic.cloudnet.ext.bridge.velocity.event.VelocityCloudServiceDisconnectNetworkEvent;
import de.dytanic.cloudnet.ext.bridge.velocity.event.VelocityCloudServiceInfoUpdateEvent;
import de.dytanic.cloudnet.ext.bridge.velocity.event.VelocityCloudServiceRegisterEvent;
import de.dytanic.cloudnet.ext.bridge.velocity.event.VelocityCloudServiceStartEvent;
import de.dytanic.cloudnet.ext.bridge.velocity.event.VelocityCloudServiceStopEvent;
import de.dytanic.cloudnet.ext.bridge.velocity.event.VelocityCloudServiceUnregisterEvent;
import de.dytanic.cloudnet.ext.bridge.velocity.event.VelocityNetworkChannelPacketReceiveEvent;
import de.dytanic.cloudnet.ext.bridge.velocity.event.VelocityServiceInfoSnapshotConfigureEvent;
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
      if (event.getServiceInfo().getProperties().contains("Online-Mode") && event.getServiceInfo().getProperties()
        .getBoolean("Online-Mode")) {
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
    this.velocityCall(new VelocityBridgeProxyPlayerServerConnectRequestEvent(event.getNetworkConnectionInfo(),
      event.getNetworkServiceInfo()));
  }

  @EventListener
  public void handle(BridgeProxyPlayerServerSwitchEvent event) {
    this.velocityCall(
      new VelocityBridgeProxyPlayerServerSwitchEvent(event.getNetworkConnectionInfo(), event.getNetworkServiceInfo()));
  }

  @EventListener
  public void handle(BridgeProxyPlayerDisconnectEvent event) {
    this.velocityCall(new VelocityBridgeProxyPlayerDisconnectEvent(event.getNetworkConnectionInfo()));
  }

  @EventListener
  public void handle(BridgeServerPlayerLoginRequestEvent event) {
    this.velocityCall(new VelocityBridgeServerPlayerLoginRequestEvent(event.getNetworkConnectionInfo(),
      event.getNetworkPlayerServerInfo()));
  }

  @EventListener
  public void handle(BridgeServerPlayerLoginSuccessEvent event) {
    this.velocityCall(new VelocityBridgeServerPlayerLoginSuccessEvent(event.getNetworkConnectionInfo(),
      event.getNetworkPlayerServerInfo()));
  }

  @EventListener
  public void handle(BridgeServerPlayerDisconnectEvent event) {
    this.velocityCall(new VelocityBridgeServerPlayerDisconnectEvent(event.getNetworkConnectionInfo(),
      event.getNetworkPlayerServerInfo()));
  }

  private <E> CompletableFuture<E> velocityCall(E event) {
    return VelocityCloudNetHelper.getProxyServer().getEventManager().fire(event);
  }

}
