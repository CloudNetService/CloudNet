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

package de.dytanic.cloudnet.ext.bridge.node.network;

import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.IEventManager;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.ext.bridge.BridgeManagement;
import de.dytanic.cloudnet.ext.bridge.event.BridgeDeleteCloudOfflinePlayerEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeProxyPlayerDisconnectEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeProxyPlayerLoginEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeProxyPlayerServerSwitchEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeServerPlayerDisconnectEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeServerPlayerLoginEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeUpdateCloudOfflinePlayerEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeUpdateCloudPlayerEvent;
import de.dytanic.cloudnet.ext.bridge.node.event.LocalPlayerPreLoginEvent;
import de.dytanic.cloudnet.ext.bridge.node.event.LocalPlayerPreLoginEvent.Result;
import de.dytanic.cloudnet.ext.bridge.node.player.NodePlayerManager;
import de.dytanic.cloudnet.ext.bridge.player.CloudOfflinePlayer;
import de.dytanic.cloudnet.ext.bridge.player.CloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.NetworkPlayerProxyInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkServiceInfo;
import java.util.Locale;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public final class NodePlayerChannelMessageListener {

  private final IEventManager eventManager;
  private final NodePlayerManager playerManager;
  private final BridgeManagement bridgeManagement;

  public NodePlayerChannelMessageListener(
    @NotNull IEventManager eventManager,
    @NotNull NodePlayerManager playerManager,
    @NotNull BridgeManagement bridgeManagement
  ) {
    this.eventManager = eventManager;
    this.playerManager = playerManager;
    this.bridgeManagement = bridgeManagement;
  }

  @EventListener
  public void handle(@NotNull ChannelMessageReceiveEvent event) {
    if (event.getChannel().equals(BridgeManagement.BRIDGE_PLAYER_CHANNEL_NAME) && event.getMessage() != null) {
      // a message regarding a player event
      switch (event.getMessage()) {
        // player login on a local proxy instance
        case "proxy_player_pre_login": {
          // read the player information
          var info = event.getContent().readObject(NetworkPlayerProxyInfo.class);
          // create the event
          var preLoginEvent = new LocalPlayerPreLoginEvent(info);
          // set the event cancelled by default if the player is already connected
          if (this.playerManager.getOnlinePlayer(info.getUniqueId()) != null) {
            preLoginEvent.setResult(Result.denied(Component.text(this.bridgeManagement.getConfiguration().getMessage(
              Locale.ENGLISH,
              "already-connected"))));
          }
          // publish the event
          var result = this.eventManager.callEvent(preLoginEvent).getResult();
          event.setBinaryResponse(DataBuf.empty().writeObject(result));
        }
        break;
        // offline player update
        case "update_offline_cloud_player": {
          // read the player
          var player = event.getContent().readObject(CloudOfflinePlayer.class);
          // push the change
          this.playerManager.pushOfflinePlayerCache(player.getUniqueId(), player);
          this.eventManager.callEvent(new BridgeUpdateCloudOfflinePlayerEvent(player));
        }
        break;
        // online player update
        case "update_online_cloud_player": {
          // read the player
          var player = event.getContent().readObject(CloudPlayer.class);
          // push the change
          this.playerManager.pushOnlinePlayerCache(player);
          this.eventManager.callEvent(new BridgeUpdateCloudPlayerEvent(player));
        }
        break;
        // offline player delete
        case "delete_offline_cloud_player": {
          // read the player
          var player = event.getContent().readObject(CloudOfflinePlayer.class);
          // push the change
          this.playerManager.pushOfflinePlayerCache(player.getUniqueId(), null);
          this.eventManager.callEvent(new BridgeDeleteCloudOfflinePlayerEvent(player));
        }
        break;
        // player login request
        case "process_cloud_player_login": {
          // read the player
          var player = event.getContent().readObject(CloudPlayer.class);
          // push the change
          this.playerManager.processLoginMessage(player);
          this.eventManager.callEvent(new BridgeProxyPlayerLoginEvent(player));
        }
        break;
        // player disconnection
        case "process_cloud_player_logout": {
          // read the player
          var player = event.getContent().readObject(CloudPlayer.class);
          // push the change
          this.playerManager.getOnlinePlayers().remove(player.getUniqueId());
          this.playerManager.pushOfflinePlayerCache(player.getUniqueId(), CloudOfflinePlayer.offlineCopy(player));
          // call the event locally
          this.eventManager.callEvent(new BridgeProxyPlayerDisconnectEvent(player));
        }
        break;
        // proxy player platform login
        case "proxy_player_login": {
          // read the info
          var info = event.getContent().readObject(NetworkPlayerProxyInfo.class);
          // process the login
          this.playerManager.loginPlayer(info, null);
        }
        break;
        // proxy player server switch
        case "proxy_player_service_switch": {
          // read the information
          var uniqueId = event.getContent().readUniqueId();
          var target = event.getContent().readObject(NetworkServiceInfo.class);
          // get the associated player
          var player = this.playerManager.getOnlinePlayer(uniqueId);
          // check if we know the player
          if (player != null) {
            // the previous service
            var prev = player.getConnectedService();
            // set the current connected service and fire the event
            player.setConnectedService(target);
            this.eventManager.callEvent(new BridgeProxyPlayerServerSwitchEvent(player, prev));
            // redirect to the cluster
            ChannelMessage.builder()
              .targetAll()
              .message("cloud_player_service_switch")
              .channel(BridgeManagement.BRIDGE_PLAYER_CHANNEL_NAME)
              .buffer(DataBuf.empty().writeObject(player).writeObject(prev))
              .build()
              .send();
          }
        }
        break;
        // redirected player service switch
        case "cloud_player_service_switch": {
          // read the information
          var player = event.getContent().readObject(CloudPlayer.class);
          var previous = event.getContent().readObject(NetworkServiceInfo.class);
          // fire the event
          this.eventManager.callEvent(new BridgeProxyPlayerServerSwitchEvent(player, previous));
        }
        break;
        // proxy player disconnect
        case "proxy_player_disconnect": {
          // read the information
          var uniqueId = event.getContent().readUniqueId();
          this.playerManager.logoutPlayer(uniqueId, null, null);
        }
        break;
        // server player login
        case "server_player_login": {
          // read the information
          var playerUniqueId = event.getContent().readUniqueId();
          var info = event.getContent().readObject(NetworkServiceInfo.class);
          // get the cloud player if known
          var player = this.playerManager.getOnlinePlayer(playerUniqueId);
          if (player != null) {
            // call the event
            this.eventManager.callEvent(new BridgeServerPlayerLoginEvent(player, info));
            // redirect to the cluster
            // redirect to the cluster
            ChannelMessage.builder()
              .targetAll()
              .message("cloud_player_server_login")
              .channel(BridgeManagement.BRIDGE_PLAYER_CHANNEL_NAME)
              .buffer(DataBuf.empty().writeObject(player).writeObject(info))
              .build()
              .send();
          }
        }
        break;
        // server player disconnect
        case "server_player_disconnect": {
          // read the information
          var playerUniqueId = event.getContent().readUniqueId();
          var info = event.getContent().readObject(NetworkServiceInfo.class);
          // get the cloud player if known
          var player = this.playerManager.getOnlinePlayer(playerUniqueId);
          if (player != null) {
            // call the event
            this.eventManager.callEvent(new BridgeServerPlayerDisconnectEvent(player, info));
            // redirect to the cluster
            // redirect to the cluster
            ChannelMessage.builder()
              .targetAll()
              .message("cloud_player_server_disconnect")
              .channel(BridgeManagement.BRIDGE_PLAYER_CHANNEL_NAME)
              .buffer(DataBuf.empty().writeObject(player).writeObject(info))
              .build()
              .send();
          }
        }
        break;
        // player server login
        case "cloud_player_server_login": {
          // read the information
          var player = event.getContent().readObject(CloudPlayer.class);
          var serviceInfo = event.getContent().readObject(NetworkServiceInfo.class);
          // fire the event
          this.eventManager.callEvent(new BridgeServerPlayerLoginEvent(player, serviceInfo));
        }
        break;
        // player server disconnect
        case "cloud_player_server_disconnect": {
          // read the information
          var player = event.getContent().readObject(CloudPlayer.class);
          var serviceInfo = event.getContent().readObject(NetworkServiceInfo.class);
          // fire the event
          this.eventManager.callEvent(new BridgeServerPlayerDisconnectEvent(player, serviceInfo));
        }
        break;
        default:
          break;
      }
    }
  }
}
