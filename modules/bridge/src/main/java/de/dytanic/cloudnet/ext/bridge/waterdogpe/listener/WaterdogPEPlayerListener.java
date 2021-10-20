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

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.proxy.BridgeProxyHelper;
import de.dytanic.cloudnet.ext.bridge.waterdogpe.WaterdogPECloudNetHelper;
import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.event.EventManager;
import dev.waterdog.waterdogpe.event.defaults.PlayerDisconnectEvent;
import dev.waterdog.waterdogpe.event.defaults.PlayerLoginEvent;
import dev.waterdog.waterdogpe.event.defaults.PlayerPreLoginEvent;
import dev.waterdog.waterdogpe.event.defaults.PostTransferCompleteEvent;
import dev.waterdog.waterdogpe.event.defaults.PreTransferEvent;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;

public final class WaterdogPEPlayerListener {

  public WaterdogPEPlayerListener() {
    EventManager eventManager = ProxyServer.getInstance().getEventManager();

    eventManager.subscribe(PlayerPreLoginEvent.class, event -> {
      String kickReason = BridgeHelper.sendChannelMessageProxyLoginRequest(
        WaterdogPECloudNetHelper.createNetworkConnectionInfo(event.getLoginData()));
      if (kickReason != null) {
        event.setCancelled(true);
        event.setCancelReason(kickReason);
      }
    });

    eventManager.subscribe(PlayerLoginEvent.class, event -> {
      BridgeHelper.sendChannelMessageProxyLoginSuccess(
        WaterdogPECloudNetHelper.createNetworkConnectionInfo(event.getPlayer().getLoginData()));
      BridgeHelper.updateServiceInfo();
    });

    eventManager.subscribe(PreTransferEvent.class, event -> {
      ProxiedPlayer proxiedPlayer = event.getPlayer();

      ServiceInfoSnapshot serviceInfoSnapshot = BridgeProxyHelper
        .getCachedServiceInfoSnapshot(event.getTargetServer().getServerName());

      if (serviceInfoSnapshot != null) {
        BridgeHelper.sendChannelMessageProxyServerConnectRequest(
          WaterdogPECloudNetHelper.createNetworkConnectionInfo(proxiedPlayer.getLoginData()),
          BridgeHelper.createNetworkServiceInfo(serviceInfoSnapshot)
        );
      }
    });

    eventManager.subscribe(PostTransferCompleteEvent.class, event -> {
      ServiceInfoSnapshot serviceInfoSnapshot = BridgeProxyHelper
        .getCachedServiceInfoSnapshot(event.getPlayer().getServerInfo().getServerName());

      if (serviceInfoSnapshot != null) {
        BridgeHelper.sendChannelMessageProxyServerSwitch(
          WaterdogPECloudNetHelper.createNetworkConnectionInfo(event.getPlayer().getLoginData()),
          BridgeHelper.createNetworkServiceInfo(serviceInfoSnapshot)
        );
      }
    });

    eventManager.subscribe(PlayerDisconnectEvent.class, event -> {
      BridgeHelper.sendChannelMessageProxyDisconnect(
        WaterdogPECloudNetHelper.createNetworkConnectionInfo(event.getPlayer().getLoginData()));
      BridgeProxyHelper.clearFallbackProfile(event.getPlayer().getUniqueId());

      ProxyServer.getInstance().getScheduler().scheduleDelayed(BridgeHelper::updateServiceInfo, 1);
    });
  }

}
