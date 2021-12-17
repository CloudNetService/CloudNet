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
    if (event.channel().equals(BridgeManagement.BRIDGE_PLAYER_CHANNEL_NAME)) {
      // a message regarding a player event
      switch (event.message()) {
        // player login on a local proxy instance
        case "proxy_player_pre_login" -> {
          // read the player information
          var info = event.content().readObject(NetworkPlayerProxyInfo.class);
          // create the event
          var preLoginEvent = new LocalPlayerPreLoginEvent(info);
          // set the event cancelled by default if the player is already connected
          if (this.playerManager.onlinePlayer(info.uniqueId()) != null) {
            preLoginEvent.result(Result.denied(Component.text(this.bridgeManagement.configuration().message(
                Locale.ENGLISH,
                "already-connected"))));
          }
          // publish the event
          var result = this.eventManager.callEvent(preLoginEvent).result();
          event.binaryResponse(DataBuf.empty().writeObject(result));
        }

        // offline player update
        case "update_offline_cloud_player" -> {
          // read the player
          var player = event.content().readObject(CloudOfflinePlayer.class);
          // push the change
          this.playerManager.pushOfflinePlayerCache(player.uniqueId(), player);
          this.eventManager.callEvent(new BridgeUpdateCloudOfflinePlayerEvent(player));
        }

        // online player update
        case "update_online_cloud_player" -> {
          // read the player
          var player = event.content().readObject(CloudPlayer.class);
          // push the change
          this.playerManager.pushOnlinePlayerCache(player);
          this.eventManager.callEvent(new BridgeUpdateCloudPlayerEvent(player));
        }

        // offline player delete
        case "delete_offline_cloud_player" -> {
          // read the player
          var player = event.content().readObject(CloudOfflinePlayer.class);
          // push the change
          this.playerManager.pushOfflinePlayerCache(player.uniqueId(), null);
          this.eventManager.callEvent(new BridgeDeleteCloudOfflinePlayerEvent(player));
        }

        // player login request
        case "process_cloud_player_login" -> {
          // read the player
          var player = event.content().readObject(CloudPlayer.class);
          // push the change
          this.playerManager.processLoginMessage(player);
          this.eventManager.callEvent(new BridgeProxyPlayerLoginEvent(player));
        }

        // player disconnection
        case "process_cloud_player_logout" -> {
          // read the player
          var player = event.content().readObject(CloudPlayer.class);
          // push the change
          this.playerManager.getOnlinePlayers().remove(player.uniqueId());
          this.playerManager.pushOfflinePlayerCache(player.uniqueId(), CloudOfflinePlayer.offlineCopy(player));
          // call the event locally
          this.eventManager.callEvent(new BridgeProxyPlayerDisconnectEvent(player));
        }

        // proxy player platform login
        case "proxy_player_login" -> {
          // read the info
          var info = event.content().readObject(NetworkPlayerProxyInfo.class);
          // process the login
          this.playerManager.loginPlayer(info, null);
        }

        // proxy player server switch
        case "proxy_player_service_switch" -> {
          // read the information
          var uniqueId = event.content().readUniqueId();
          var target = event.content().readObject(NetworkServiceInfo.class);
          // get the associated player
          var player = this.playerManager.onlinePlayer(uniqueId);
          // check if we know the player
          if (player != null) {
            // the previous service
            var prev = player.connectedService();
            // set the current connected service and fire the event
            player.connectedService(target);
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

        // redirected player service switch
        case "cloud_player_service_switch" -> {
          // read the information
          var player = event.content().readObject(CloudPlayer.class);
          var previous = event.content().readObject(NetworkServiceInfo.class);
          // fire the event
          this.eventManager.callEvent(new BridgeProxyPlayerServerSwitchEvent(player, previous));
        }

        // proxy player disconnect
        case "proxy_player_disconnect" -> {
          // read the information
          var uniqueId = event.content().readUniqueId();
          this.playerManager.logoutPlayer(uniqueId, null, null);
        }

        // server player login
        case "server_player_login" -> {
          // read the information
          var playerUniqueId = event.content().readUniqueId();
          var info = event.content().readObject(NetworkServiceInfo.class);
          // get the cloud player if known
          var player = this.playerManager.onlinePlayer(playerUniqueId);
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

        // server player disconnect
        case "server_player_disconnect" -> {
          // read the information
          var playerUniqueId = event.content().readUniqueId();
          var info = event.content().readObject(NetworkServiceInfo.class);
          // get the cloud player if known
          var player = this.playerManager.onlinePlayer(playerUniqueId);
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

        // player server login
        case "cloud_player_server_login" -> {
          // read the information
          var player = event.content().readObject(CloudPlayer.class);
          var serviceInfo = event.content().readObject(NetworkServiceInfo.class);
          // fire the event
          this.eventManager.callEvent(new BridgeServerPlayerLoginEvent(player, serviceInfo));
        }

        // player server disconnect
        case "cloud_player_server_disconnect" -> {
          // read the information
          var player = event.content().readObject(CloudPlayer.class);
          var serviceInfo = event.content().readObject(NetworkServiceInfo.class);
          // fire the event
          this.eventManager.callEvent(new BridgeServerPlayerDisconnectEvent(player, serviceInfo));
        }
        default -> {
        }
      }
    }
  }
}
