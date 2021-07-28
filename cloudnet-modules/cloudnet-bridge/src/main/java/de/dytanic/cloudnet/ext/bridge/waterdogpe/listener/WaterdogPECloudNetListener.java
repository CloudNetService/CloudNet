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

package de.dytanic.cloudnet.ext.bridge.waterdogpe.listener;

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
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
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
import de.dytanic.cloudnet.ext.bridge.waterdogpe.WaterdogPECloudNetHelper;
import de.dytanic.cloudnet.ext.bridge.waterdogpe.event.WaterdogPEBridgeConfigurationUpdateEvent;
import de.dytanic.cloudnet.ext.bridge.waterdogpe.event.WaterdogPEBridgeProxyPlayerDisconnectEvent;
import de.dytanic.cloudnet.ext.bridge.waterdogpe.event.WaterdogPEBridgeProxyPlayerLoginRequestEvent;
import de.dytanic.cloudnet.ext.bridge.waterdogpe.event.WaterdogPEBridgeProxyPlayerLoginSuccessEvent;
import de.dytanic.cloudnet.ext.bridge.waterdogpe.event.WaterdogPEBridgeProxyPlayerServerConnectRequestEvent;
import de.dytanic.cloudnet.ext.bridge.waterdogpe.event.WaterdogPEBridgeProxyPlayerServerSwitchEvent;
import de.dytanic.cloudnet.ext.bridge.waterdogpe.event.WaterdogPEBridgeServerPlayerDisconnectEvent;
import de.dytanic.cloudnet.ext.bridge.waterdogpe.event.WaterdogPEBridgeServerPlayerLoginRequestEvent;
import de.dytanic.cloudnet.ext.bridge.waterdogpe.event.WaterdogPEBridgeServerPlayerLoginSuccessEvent;
import de.dytanic.cloudnet.ext.bridge.waterdogpe.event.WaterdogPEChannelMessageReceiveEvent;
import de.dytanic.cloudnet.ext.bridge.waterdogpe.event.WaterdogPECloudServiceConnectNetworkEvent;
import de.dytanic.cloudnet.ext.bridge.waterdogpe.event.WaterdogPECloudServiceDisconnectNetworkEvent;
import de.dytanic.cloudnet.ext.bridge.waterdogpe.event.WaterdogPECloudServiceInfoUpdateEvent;
import de.dytanic.cloudnet.ext.bridge.waterdogpe.event.WaterdogPECloudServiceRegisterEvent;
import de.dytanic.cloudnet.ext.bridge.waterdogpe.event.WaterdogPECloudServiceStartEvent;
import de.dytanic.cloudnet.ext.bridge.waterdogpe.event.WaterdogPECloudServiceStopEvent;
import de.dytanic.cloudnet.ext.bridge.waterdogpe.event.WaterdogPECloudServiceUnregisterEvent;
import de.dytanic.cloudnet.ext.bridge.waterdogpe.event.WaterdogPENetworkChannelPacketReceiveEvent;
import de.dytanic.cloudnet.ext.bridge.waterdogpe.event.WaterdogPEServiceInfoSnapshotConfigureEvent;
import de.dytanic.cloudnet.wrapper.event.service.ServiceInfoSnapshotConfigureEvent;
import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.event.Event;
import dev.waterdog.waterdogpe.network.serverinfo.BedrockServerInfo;
import java.net.InetSocketAddress;

public final class WaterdogPECloudNetListener {

  @EventListener
  public void handle(ServiceInfoSnapshotConfigureEvent event) {
    WaterdogPECloudNetHelper.initProperties(event.getServiceInfoSnapshot());
    this.waterdogPECall(new WaterdogPEServiceInfoSnapshotConfigureEvent(event.getServiceInfoSnapshot()));
  }

  @EventListener
  public void handle(CloudServiceStartEvent event) {
    ServiceInfoSnapshot serviceInfoSnapshot = event.getServiceInfo();

    if (serviceInfoSnapshot.getServiceId().getEnvironment().isMinecraftBedrockServer()) {
      if (serviceInfoSnapshot.getProperties().contains("Online-Mode") && event.getServiceInfo().getProperties()
        .getBoolean("Online-Mode")) {
        return;
      }

      String name = serviceInfoSnapshot.getServiceId().getName();
      InetSocketAddress address = new InetSocketAddress(
        serviceInfoSnapshot.getConnectAddress().getHost(),
        serviceInfoSnapshot.getConnectAddress().getPort()
      );

      ProxyServer.getInstance().getScheduler().scheduleTask(() ->
        ProxyServer.getInstance().registerServerInfo(new BedrockServerInfo(name, address, address)), false);
    }

    this.waterdogPECall(new WaterdogPECloudServiceStartEvent(event.getServiceInfo()));
  }

  @EventListener
  public void handle(CloudServiceStopEvent event) {
    ServiceInfoSnapshot serviceInfoSnapshot = event.getServiceInfo();

    if (serviceInfoSnapshot.getServiceId().getEnvironment().isMinecraftBedrockServer()) {
      ProxyServer.getInstance().getScheduler().scheduleTask(() ->
        ProxyServer.getInstance().removeServerInfo(serviceInfoSnapshot.getName()), false);
      BridgeProxyHelper.cacheServiceInfoSnapshot(serviceInfoSnapshot);
    }

    this.waterdogPECall(new WaterdogPECloudServiceStopEvent(event.getServiceInfo()));
  }

  @EventListener
  public void handle(CloudServiceInfoUpdateEvent event) {
    ServiceInfoSnapshot serviceInfoSnapshot = event.getServiceInfo();

    if (serviceInfoSnapshot.getServiceId().getEnvironment().isMinecraftBedrockServer()) {
      BridgeProxyHelper.cacheServiceInfoSnapshot(serviceInfoSnapshot);
    }

    this.waterdogPECall(new WaterdogPECloudServiceInfoUpdateEvent(event.getServiceInfo()));
  }

  @EventListener
  public void handle(CloudServiceRegisterEvent event) {
    ServiceInfoSnapshot serviceInfoSnapshot = event.getServiceInfo();

    if (serviceInfoSnapshot.getServiceId().getEnvironment().isMinecraftBedrockServer()) {
      BridgeProxyHelper.cacheServiceInfoSnapshot(serviceInfoSnapshot);
    }

    this.waterdogPECall(new WaterdogPECloudServiceRegisterEvent(event.getServiceInfo()));
  }

  @EventListener
  public void handle(CloudServiceConnectNetworkEvent event) {
    ServiceInfoSnapshot serviceInfoSnapshot = event.getServiceInfo();

    if (serviceInfoSnapshot.getServiceId().getEnvironment().isMinecraftBedrockServer()) {
      BridgeProxyHelper.cacheServiceInfoSnapshot(serviceInfoSnapshot);
    }

    this.waterdogPECall(new WaterdogPECloudServiceConnectNetworkEvent(event.getServiceInfo()));
  }

  @EventListener
  public void handle(CloudServiceDisconnectNetworkEvent event) {
    ServiceInfoSnapshot serviceInfoSnapshot = event.getServiceInfo();

    if (serviceInfoSnapshot.getServiceId().getEnvironment().isMinecraftBedrockServer()) {
      BridgeProxyHelper.cacheServiceInfoSnapshot(serviceInfoSnapshot);
    }

    this.waterdogPECall(new WaterdogPECloudServiceDisconnectNetworkEvent(event.getServiceInfo()));
  }

  @EventListener
  public void handle(CloudServiceUnregisterEvent event) {
    ServiceInfoSnapshot serviceInfoSnapshot = event.getServiceInfo();

    if (serviceInfoSnapshot.getServiceId().getEnvironment().isMinecraftBedrockServer()) {
      BridgeProxyHelper.removeCachedServiceInfoSnapshot(serviceInfoSnapshot);
    }

    this.waterdogPECall(new WaterdogPECloudServiceUnregisterEvent(event.getServiceInfo()));
  }

  @EventListener
  public void handle(ChannelMessageReceiveEvent event) {
    this.waterdogPECall(new WaterdogPEChannelMessageReceiveEvent(event));
  }

  @EventListener
  public void handle(NetworkChannelPacketReceiveEvent event) {
    this.waterdogPECall(new WaterdogPENetworkChannelPacketReceiveEvent(event.getChannel(), event.getPacket()));
  }

  @EventListener
  public void handle(BridgeConfigurationUpdateEvent event) {
    this.waterdogPECall(new WaterdogPEBridgeConfigurationUpdateEvent(event.getBridgeConfiguration()));
  }

  @EventListener
  public void handle(BridgeProxyPlayerLoginRequestEvent event) {
    this.waterdogPECall(new WaterdogPEBridgeProxyPlayerLoginRequestEvent(event.getNetworkConnectionInfo()));
  }

  @EventListener
  public void handle(BridgeProxyPlayerLoginSuccessEvent event) {
    this.waterdogPECall(new WaterdogPEBridgeProxyPlayerLoginSuccessEvent(event.getNetworkConnectionInfo()));
  }

  @EventListener
  public void handle(BridgeProxyPlayerServerConnectRequestEvent event) {
    this.waterdogPECall(new WaterdogPEBridgeProxyPlayerServerConnectRequestEvent(event.getNetworkConnectionInfo(),
      event.getNetworkServiceInfo()));
  }

  @EventListener
  public void handle(BridgeProxyPlayerServerSwitchEvent event) {
    this.waterdogPECall(new WaterdogPEBridgeProxyPlayerServerSwitchEvent(event.getNetworkConnectionInfo(),
      event.getNetworkServiceInfo()));
  }

  @EventListener
  public void handle(BridgeProxyPlayerDisconnectEvent event) {
    this.waterdogPECall(new WaterdogPEBridgeProxyPlayerDisconnectEvent(event.getNetworkConnectionInfo()));
  }

  @EventListener
  public void handle(BridgeServerPlayerLoginRequestEvent event) {
    this.waterdogPECall(new WaterdogPEBridgeServerPlayerLoginRequestEvent(event.getNetworkConnectionInfo(),
      event.getNetworkPlayerServerInfo()));
  }

  @EventListener
  public void handle(BridgeServerPlayerLoginSuccessEvent event) {
    this.waterdogPECall(new WaterdogPEBridgeServerPlayerLoginSuccessEvent(event.getNetworkConnectionInfo(),
      event.getNetworkPlayerServerInfo()));
  }

  @EventListener
  public void handle(BridgeServerPlayerDisconnectEvent event) {
    this.waterdogPECall(new WaterdogPEBridgeServerPlayerDisconnectEvent(event.getNetworkConnectionInfo(),
      event.getNetworkPlayerServerInfo()));
  }

  private void waterdogPECall(Event event) {
    ProxyServer.getInstance().getEventManager().callEvent(event);
  }
}
