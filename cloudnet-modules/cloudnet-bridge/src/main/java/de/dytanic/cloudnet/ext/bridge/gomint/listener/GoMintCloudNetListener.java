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

package de.dytanic.cloudnet.ext.bridge.gomint.listener;

import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.concurrent.CompletedTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
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
import de.dytanic.cloudnet.ext.bridge.gomint.GoMintCloudNetHelper;
import de.dytanic.cloudnet.ext.bridge.gomint.event.GoMintBridgeConfigurationUpdateEvent;
import de.dytanic.cloudnet.ext.bridge.gomint.event.GoMintBridgeProxyPlayerDisconnectEvent;
import de.dytanic.cloudnet.ext.bridge.gomint.event.GoMintBridgeProxyPlayerLoginSuccessEvent;
import de.dytanic.cloudnet.ext.bridge.gomint.event.GoMintBridgeProxyPlayerServerConnectRequestEvent;
import de.dytanic.cloudnet.ext.bridge.gomint.event.GoMintBridgeProxyPlayerServerSwitchEvent;
import de.dytanic.cloudnet.ext.bridge.gomint.event.GoMintBridgeServerPlayerDisconnectEvent;
import de.dytanic.cloudnet.ext.bridge.gomint.event.GoMintBridgeServerPlayerLoginRequestEvent;
import de.dytanic.cloudnet.ext.bridge.gomint.event.GoMintBridgeServerPlayerLoginSuccessEvent;
import de.dytanic.cloudnet.ext.bridge.gomint.event.GoMintChannelMessageReceiveEvent;
import de.dytanic.cloudnet.ext.bridge.gomint.event.GoMintCloudServiceConnectNetworkEvent;
import de.dytanic.cloudnet.ext.bridge.gomint.event.GoMintCloudServiceDisconnectNetworkEvent;
import de.dytanic.cloudnet.ext.bridge.gomint.event.GoMintCloudServiceInfoUpdateEvent;
import de.dytanic.cloudnet.ext.bridge.gomint.event.GoMintCloudServiceRegisterEvent;
import de.dytanic.cloudnet.ext.bridge.gomint.event.GoMintCloudServiceStartEvent;
import de.dytanic.cloudnet.ext.bridge.gomint.event.GoMintCloudServiceStopEvent;
import de.dytanic.cloudnet.ext.bridge.gomint.event.GoMintCloudServiceUnregisterEvent;
import de.dytanic.cloudnet.ext.bridge.gomint.event.GoMintNetworkChannelPacketReceiveEvent;
import de.dytanic.cloudnet.ext.bridge.gomint.event.GoMintServiceInfoSnapshotConfigureEvent;
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
      GoMint.instance().pluginManager()
        .callEvent(new GoMintServiceInfoSnapshotConfigureEvent(event.getServiceInfoSnapshot()));
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
    this.goMintCall(new GoMintBridgeProxyPlayerServerConnectRequestEvent(event.getNetworkConnectionInfo(),
      event.getNetworkServiceInfo()));
  }

  @EventListener
  public void handle(BridgeProxyPlayerServerSwitchEvent event) {
    this.goMintCall(
      new GoMintBridgeProxyPlayerServerSwitchEvent(event.getNetworkConnectionInfo(), event.getNetworkServiceInfo()));
  }

  @EventListener
  public void handle(BridgeProxyPlayerDisconnectEvent event) {
    this.goMintCall(new GoMintBridgeProxyPlayerDisconnectEvent(event.getNetworkConnectionInfo()));
  }

  @EventListener
  public void handle(BridgeServerPlayerLoginRequestEvent event) {
    this.goMintCall(new GoMintBridgeServerPlayerLoginRequestEvent(event.getNetworkConnectionInfo(),
      event.getNetworkPlayerServerInfo()));
  }

  @EventListener
  public void handle(BridgeServerPlayerLoginSuccessEvent event) {
    this.goMintCall(new GoMintBridgeServerPlayerLoginSuccessEvent(event.getNetworkConnectionInfo(),
      event.getNetworkPlayerServerInfo()));
  }

  @EventListener
  public void handle(BridgeServerPlayerDisconnectEvent event) {
    this.goMintCall(new GoMintBridgeServerPlayerDisconnectEvent(event.getNetworkConnectionInfo(),
      event.getNetworkPlayerServerInfo()));
  }

  private void goMintCall(Event event) {
    GoMint.instance().pluginManager().callEvent(event);
  }

  private ITask<Void> listenableGoMintSyncExecution(Runnable runnable) {
    if (GoMint.instance().mainThread()) {
      runnable.run();
      return CompletedTask.voidTask();
    }

    CompletableTask<Void> task = new CompletableTask<>();
    this.plugin.scheduler().execute(runnable).onComplete(() -> task.complete(null));

    return task;
  }

}
