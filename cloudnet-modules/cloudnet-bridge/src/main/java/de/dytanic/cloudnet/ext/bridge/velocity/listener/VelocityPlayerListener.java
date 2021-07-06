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

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.proxy.BridgeProxyHelper;
import de.dytanic.cloudnet.ext.bridge.velocity.VelocityCloudNetBridgePlugin;
import de.dytanic.cloudnet.ext.bridge.velocity.VelocityCloudNetHelper;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class VelocityPlayerListener {

  private final VelocityCloudNetBridgePlugin plugin;

  public VelocityPlayerListener(VelocityCloudNetBridgePlugin plugin) {
    this.plugin = plugin;
  }

  @Subscribe
  public void handle(LoginEvent event) {
    String kickReason = BridgeHelper
      .sendChannelMessageProxyLoginRequest(VelocityCloudNetHelper.createNetworkConnectionInfo(event.getPlayer()));
    if (kickReason != null) {
      Component reason = LegacyComponentSerializer.legacySection().deserialize(kickReason);
      event.setResult(ResultedEvent.ComponentResult.denied(reason));
    }
  }

  @Subscribe
  public void handle(PostLoginEvent event) {
    BridgeHelper
      .sendChannelMessageProxyLoginSuccess(VelocityCloudNetHelper.createNetworkConnectionInfo(event.getPlayer()));

    VelocityCloudNetHelper.getProxyServer().getScheduler()
      .buildTask(this.plugin, VelocityCloudNetHelper::updateServiceInfo)
      .delay(50, TimeUnit.MILLISECONDS).schedule();
  }

  @Subscribe
  public void handle(ServerPreConnectEvent event) {
    if (!event.getPlayer().getCurrentServer().isPresent()) {
      VelocityCloudNetHelper.getNextFallback(event.getPlayer())
        .ifPresent(registeredServer -> event.setResult(ServerPreConnectEvent.ServerResult.allowed(registeredServer)));
    }

    ServiceInfoSnapshot serviceInfoSnapshot = BridgeProxyHelper
      .getCachedServiceInfoSnapshot(event.getResult().getServer().get().getServerInfo().getName());

    if (serviceInfoSnapshot != null) {
      BridgeHelper.sendChannelMessageProxyServerConnectRequest(
        VelocityCloudNetHelper.createNetworkConnectionInfo(event.getPlayer()),
        BridgeHelper.createNetworkServiceInfo(serviceInfoSnapshot)
      );
    }
  }

  @Subscribe
  public void handle(ServerConnectedEvent event) {
    ServiceInfoSnapshot serviceInfoSnapshot = BridgeProxyHelper
      .getCachedServiceInfoSnapshot(event.getServer().getServerInfo().getName());

    if (serviceInfoSnapshot != null) {
      BridgeHelper.sendChannelMessageProxyServerSwitch(
        VelocityCloudNetHelper.createNetworkConnectionInfo(event.getPlayer()),
        BridgeHelper.createNetworkServiceInfo(serviceInfoSnapshot)
      );
    }
  }

  @Subscribe
  public void handle(KickedFromServerEvent event) {
    if (event.getPlayer().isActive()) {
      BridgeProxyHelper
        .handleConnectionFailed(event.getPlayer().getUniqueId(), event.getServer().getServerInfo().getName());

      VelocityCloudNetHelper.getNextFallback(event.getPlayer())
        .ifPresent(registeredServer -> event.setResult(KickedFromServerEvent.RedirectPlayer.create(registeredServer)));
    }
  }

  @Subscribe
  public void handle(DisconnectEvent event) {
    BridgeHelper
      .sendChannelMessageProxyDisconnect(VelocityCloudNetHelper.createNetworkConnectionInfo(event.getPlayer()));
    BridgeProxyHelper.clearFallbackProfile(event.getPlayer().getUniqueId());

    VelocityCloudNetHelper.getProxyServer().getScheduler()
      .buildTask(this.plugin, VelocityCloudNetHelper::updateServiceInfo)
      .delay(50, TimeUnit.MILLISECONDS).schedule();
  }
}
