/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.bridge.node.network;

import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.event.events.channel.ChannelMessageReceiveEvent;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.ext.component.ComponentFormats;
import eu.cloudnetservice.modules.bridge.BridgeManagement;
import eu.cloudnetservice.modules.bridge.event.BridgeDeleteCloudOfflinePlayerEvent;
import eu.cloudnetservice.modules.bridge.event.BridgeProxyPlayerDisconnectEvent;
import eu.cloudnetservice.modules.bridge.event.BridgeProxyPlayerLoginEvent;
import eu.cloudnetservice.modules.bridge.event.BridgeProxyPlayerServerSwitchEvent;
import eu.cloudnetservice.modules.bridge.event.BridgeServerPlayerDisconnectEvent;
import eu.cloudnetservice.modules.bridge.event.BridgeServerPlayerLoginEvent;
import eu.cloudnetservice.modules.bridge.event.BridgeUpdateCloudOfflinePlayerEvent;
import eu.cloudnetservice.modules.bridge.event.BridgeUpdateCloudPlayerEvent;
import eu.cloudnetservice.modules.bridge.node.event.LocalPlayerPreLoginEvent;
import eu.cloudnetservice.modules.bridge.node.player.NodePlayerManager;
import eu.cloudnetservice.modules.bridge.player.CloudOfflinePlayer;
import eu.cloudnetservice.modules.bridge.player.CloudPlayer;
import eu.cloudnetservice.modules.bridge.player.NetworkPlayerProxyInfo;
import eu.cloudnetservice.modules.bridge.player.NetworkPlayerServerInfo;
import eu.cloudnetservice.modules.bridge.player.NetworkServiceInfo;
import jakarta.inject.Singleton;
import java.util.Locale;
import lombok.NonNull;
import net.kyori.adventure.text.Component;

@Singleton
public final class NodePlayerChannelMessageListener {

  @EventListener
  public void handle(
    @NonNull ChannelMessageReceiveEvent event,
    @NonNull EventManager eventManager,
    @NonNull NodePlayerManager playerManager,
    @NonNull BridgeManagement bridgeManagement
  ) {
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
          if (playerManager.onlinePlayer(info.uniqueId()) != null) {
            preLoginEvent.result(bridgeManagement.configuration().findMessage(
              Locale.ENGLISH,
              "already-connected",
              message -> {
                var component = ComponentFormats.BUNGEE_TO_ADVENTURE.convert(message);
                return LocalPlayerPreLoginEvent.Result.denied(component);
              },
              LocalPlayerPreLoginEvent.Result.denied(Component.empty()),
              true));
          }
          // publish the event
          var result = eventManager.callEvent(preLoginEvent).result();
          event.binaryResponse(DataBuf.empty().writeObject(result));
        }

        // offline player update
        case "update_offline_cloud_player" -> {
          // read the player
          var player = event.content().readObject(CloudOfflinePlayer.class);
          // push the change
          playerManager.pushOfflinePlayerCache(player.uniqueId(), player);
          eventManager.callEvent(new BridgeUpdateCloudOfflinePlayerEvent(player));
        }

        // online player update
        case "update_online_cloud_player" -> {
          // read the player
          var player = event.content().readObject(CloudPlayer.class);
          // push the change
          playerManager.pushOnlinePlayerCache(player);
          eventManager.callEvent(new BridgeUpdateCloudPlayerEvent(player));
        }

        // offline player delete
        case "delete_offline_cloud_player" -> {
          // read the player
          var player = event.content().readObject(CloudOfflinePlayer.class);
          // push the change
          playerManager.pushOfflinePlayerCache(player.uniqueId(), null);
          eventManager.callEvent(new BridgeDeleteCloudOfflinePlayerEvent(player));
        }

        // player login request
        case "process_cloud_player_login" -> {
          // read the player
          var player = event.content().readObject(CloudPlayer.class);
          // push the change
          playerManager.processLoginMessage(player);
          eventManager.callEvent(new BridgeProxyPlayerLoginEvent(player));
        }

        // player disconnection
        case "process_cloud_player_logout" -> {
          // read the player
          var player = event.content().readObject(CloudPlayer.class);
          // push the change
          playerManager.players().remove(player.uniqueId());
          playerManager.pushOfflinePlayerCache(player.uniqueId(), CloudOfflinePlayer.offlineCopy(player));
          // call the event locally
          eventManager.callEvent(new BridgeProxyPlayerDisconnectEvent(player));
        }

        // proxy player platform login
        case "proxy_player_login" -> {
          // read the info
          var proxy = event.content().readObject(NetworkPlayerProxyInfo.class);
          var joinedService = event.content().readObject(NetworkServiceInfo.class);
          // process the login
          playerManager.loginPlayer(proxy, joinedService);
        }

        // proxy player server switch
        case "proxy_player_service_switch" -> {
          // read the information
          var uniqueId = event.content().readUniqueId();
          var target = event.content().readObject(NetworkServiceInfo.class);
          // get the associated player
          var player = playerManager.onlinePlayer(uniqueId);
          // check if we know the player
          if (player != null) {
            // the previous service
            var prev = player.connectedService();
            // set the current connected service and fire the event
            player.connectedService(target);
            eventManager.callEvent(new BridgeProxyPlayerServerSwitchEvent(player, prev));
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
          // update the cache & fire the event
          playerManager.pushOnlinePlayerCache(player);
          eventManager.callEvent(new BridgeProxyPlayerServerSwitchEvent(player, previous));
        }

        // proxy player disconnect
        case "proxy_player_disconnect" -> {
          // read the information
          var uniqueId = event.content().readUniqueId();
          playerManager.logoutPlayer(uniqueId, null, null);
        }

        // server player login
        case "server_player_login" -> {
          // read the information
          var playerUniqueId = event.content().readUniqueId();
          var info = event.content().readObject(NetworkPlayerServerInfo.class);
          // get the cloud player if known
          var player = playerManager.onlinePlayer(playerUniqueId);
          if (player != null) {
            // update the player locally & call the event
            player.networkPlayerServerInfo(info);
            eventManager.callEvent(new BridgeServerPlayerLoginEvent(player, info));
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
          var player = playerManager.onlinePlayer(playerUniqueId);
          if (player != null) {
            // call the event
            eventManager.callEvent(new BridgeServerPlayerDisconnectEvent(player, info));
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
          var serviceInfo = event.content().readObject(NetworkPlayerServerInfo.class);
          // update the local player cache & fire the event
          playerManager.pushOnlinePlayerCache(player);
          eventManager.callEvent(new BridgeServerPlayerLoginEvent(player, serviceInfo));
        }

        // player server disconnect
        case "cloud_player_server_disconnect" -> {
          // read the information
          var player = event.content().readObject(CloudPlayer.class);
          var serviceInfo = event.content().readObject(NetworkServiceInfo.class);
          // fire the event
          eventManager.callEvent(new BridgeServerPlayerDisconnectEvent(player, serviceInfo));
        }

        // none of our business - just ignore that
        default -> {
        }
      }
    }
  }
}
