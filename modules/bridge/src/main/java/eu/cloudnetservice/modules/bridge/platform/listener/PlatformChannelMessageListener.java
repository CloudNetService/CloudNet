/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.bridge.platform.listener;

import eu.cloudnetservice.cloudnet.driver.event.EventListener;
import eu.cloudnetservice.cloudnet.driver.event.EventManager;
import eu.cloudnetservice.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBuf;
import eu.cloudnetservice.modules.bridge.BridgeManagement;
import eu.cloudnetservice.modules.bridge.config.BridgeConfiguration;
import eu.cloudnetservice.modules.bridge.event.BridgeDeleteCloudOfflinePlayerEvent;
import eu.cloudnetservice.modules.bridge.event.BridgeProxyPlayerDisconnectEvent;
import eu.cloudnetservice.modules.bridge.event.BridgeProxyPlayerLoginEvent;
import eu.cloudnetservice.modules.bridge.event.BridgeProxyPlayerServerSwitchEvent;
import eu.cloudnetservice.modules.bridge.event.BridgeServerPlayerDisconnectEvent;
import eu.cloudnetservice.modules.bridge.event.BridgeServerPlayerLoginEvent;
import eu.cloudnetservice.modules.bridge.event.BridgeUpdateCloudOfflinePlayerEvent;
import eu.cloudnetservice.modules.bridge.event.BridgeUpdateCloudPlayerEvent;
import eu.cloudnetservice.modules.bridge.platform.PlatformBridgeManagement;
import eu.cloudnetservice.modules.bridge.player.CloudOfflinePlayer;
import eu.cloudnetservice.modules.bridge.player.CloudPlayer;
import eu.cloudnetservice.modules.bridge.player.NetworkPlayerServerInfo;
import eu.cloudnetservice.modules.bridge.player.NetworkServiceInfo;
import eu.cloudnetservice.modules.bridge.player.executor.ServerSelectorType;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

public final class PlatformChannelMessageListener {

  private final EventManager eventManager;
  private final PlatformBridgeManagement<?, ?> management;

  public PlatformChannelMessageListener(
    @NonNull EventManager eventManager,
    @NonNull PlatformBridgeManagement<?, ?> management
  ) {
    this.eventManager = eventManager;
    this.management = management;
  }

  @EventListener
  public void handleConfigurationChannelMessage(@NonNull ChannelMessageReceiveEvent event) {
    if (event.channel().equals(BridgeManagement.BRIDGE_CHANNEL_NAME) && event.message()
      .equals("update_bridge_configuration")) {
      // read the config
      var configuration = event.content().readObject(BridgeConfiguration.class);
      // set the configuration
      this.management.configurationSilently(configuration);
    }
  }

  @EventListener
  public void handlePlayerChannelMessage(@NonNull ChannelMessageReceiveEvent event) {
    if (event.channel().equals(BridgeManagement.BRIDGE_PLAYER_CHANNEL_NAME)) {
      // a message regarding a player event
      switch (event.message()) {
        // offline player update
        case "update_offline_cloud_player" -> {
          // read the player
          var player = event.content().readObject(CloudOfflinePlayer.class);
          // push the change
          this.eventManager.callEvent(new BridgeUpdateCloudOfflinePlayerEvent(player));
        }

        // online player update
        case "update_online_cloud_player" -> {
          // read the player
          var player = event.content().readObject(CloudPlayer.class);
          // push the change
          this.eventManager.callEvent(new BridgeUpdateCloudPlayerEvent(player));
        }

        // offline player delete
        case "delete_offline_cloud_player" -> {
          // read the player
          var player = event.content().readObject(CloudOfflinePlayer.class);
          // push the change
          this.eventManager.callEvent(new BridgeDeleteCloudOfflinePlayerEvent(player));
        }

        // player login request
        case "process_cloud_player_login" -> {
          // read the player
          var player = event.content().readObject(CloudPlayer.class);
          // push the change
          this.eventManager.callEvent(new BridgeProxyPlayerLoginEvent(player));
        }

        // player disconnection
        case "process_cloud_player_logout" -> {
          // read the player
          var player = event.content().readObject(CloudPlayer.class);
          // call the event locally
          this.eventManager.callEvent(new BridgeProxyPlayerDisconnectEvent(player));
        }

        // player service switch
        case "cloud_player_service_switch" -> {
          // read the information
          var player = event.content().readObject(CloudPlayer.class);
          var previous = event.content().readObject(NetworkServiceInfo.class);
          // fire the event
          this.eventManager.callEvent(new BridgeProxyPlayerServerSwitchEvent(player, previous));
        }

        // player server login
        case "cloud_player_server_login" -> {
          // read the information
          var player = event.content().readObject(CloudPlayer.class);
          var serviceInfo = event.content().readObject(NetworkPlayerServerInfo.class);
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

  @EventListener
  public void handlePlayerExecutorChannelMessage(@NonNull ChannelMessageReceiveEvent event) {
    if (event.channel().equals(BridgeManagement.BRIDGE_PLAYER_EXECUTOR_CHANNEL_NAME)) {
      // the target unique id is always the first argument
      var executor = this.management.directPlayerExecutor(event.content().readUniqueId());
      // a message regarding a player executor request
      switch (event.message()) {
        // connect to a service
        case "connect_to_service" -> executor.connect(event.content().readString());

        // connect using the given selector
        case "connect_to_selector" -> executor.connectSelecting(
          event.content().readObject(ServerSelectorType.class));

        // connect to a fallback
        case "connect_to_fallback" -> executor.connectToFallback();

        // connect to a group
        case "connect_to_group" -> executor.connectToGroup(
          event.content().readString(),
          event.content().readObject(ServerSelectorType.class));

        // connect to a task
        case "connect_to_task" -> executor.connectToTask(
          event.content().readString(),
          event.content().readObject(ServerSelectorType.class));

        // kick a player from the current service
        case "kick_player" -> executor.kick(event.content().readObject(Component.class));

        // send a title to the player
        case "send_title" -> executor.sendTitle(event.content().readObject(Title.class));

        // send a chat message to the player
        case "send_chat_message" -> executor.sendChatMessage(
          event.content().readObject(Component.class),
          event.content().readNullable(DataBuf::readString));

        // send a plugin message to the player
        case "send_plugin_message" -> executor.sendPluginMessage(event.content().readString(),
          event.content().readByteArray());

        // dispatches the given input string as a command
        case "spoof_command_execution" -> executor.spoofCommandExecution(event.content().readString());

        // unable to handle
        default -> {
        }
      }
    }
  }
}
