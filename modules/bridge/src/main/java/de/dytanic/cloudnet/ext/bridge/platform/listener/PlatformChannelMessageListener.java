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

package de.dytanic.cloudnet.ext.bridge.platform.listener;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.IEventManager;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.ext.bridge.BridgeManagement;
import de.dytanic.cloudnet.ext.bridge.config.BridgeConfiguration;
import de.dytanic.cloudnet.ext.bridge.event.BridgeDeleteCloudOfflinePlayerEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeProxyPlayerDisconnectEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeProxyPlayerLoginEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeProxyPlayerServerSwitchEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeServerPlayerDisconnectEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeServerPlayerLoginEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeUpdateCloudOfflinePlayerEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeUpdateCloudPlayerEvent;
import de.dytanic.cloudnet.ext.bridge.platform.PlatformBridgeManagement;
import de.dytanic.cloudnet.ext.bridge.player.CloudOfflinePlayer;
import de.dytanic.cloudnet.ext.bridge.player.CloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.NetworkServiceInfo;
import de.dytanic.cloudnet.ext.bridge.player.executor.PlayerExecutor;
import de.dytanic.cloudnet.ext.bridge.player.executor.ServerSelectorType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.jetbrains.annotations.NotNull;

public final class PlatformChannelMessageListener {

  private final IEventManager eventManager;
  private final PlatformBridgeManagement<?, ?> management;

  public PlatformChannelMessageListener(
    @NotNull IEventManager eventManager,
    @NotNull PlatformBridgeManagement<?, ?> management
  ) {
    this.eventManager = eventManager;
    this.management = management;
  }

  @EventListener
  public void handleConfigurationChannelMessage(@NotNull ChannelMessageReceiveEvent event) {
    if (event.getChannel().equals(BridgeManagement.BRIDGE_CHANNEL_NAME)
      && event.getMessage() != null
      && event.getMessage().equals("update_bridge_configuration")) {
      // read the config
      var configuration = event.getContent().readObject(BridgeConfiguration.class);
      // set the configuration
      this.management.setConfigurationLocally(configuration);
    }
  }

  @EventListener
  public void handlePlayerChannelMessage(@NotNull ChannelMessageReceiveEvent event) {
    if (event.getChannel().equals(BridgeManagement.BRIDGE_PLAYER_CHANNEL_NAME) && event.getMessage() != null) {
      // a message regarding a player event
      switch (event.getMessage()) {
        // offline player update
        case "update_offline_cloud_player": {
          // read the player
          var player = event.getContent().readObject(CloudOfflinePlayer.class);
          // push the change
          this.eventManager.callEvent(new BridgeUpdateCloudOfflinePlayerEvent(player));
        }
        break;
        // online player update
        case "update_online_cloud_player": {
          // read the player
          var player = event.getContent().readObject(CloudPlayer.class);
          // push the change
          this.eventManager.callEvent(new BridgeUpdateCloudPlayerEvent(player));
        }
        break;
        // offline player delete
        case "delete_offline_cloud_player": {
          // read the player
          var player = event.getContent().readObject(CloudOfflinePlayer.class);
          // push the change
          this.eventManager.callEvent(new BridgeDeleteCloudOfflinePlayerEvent(player));
        }
        break;
        // player login request
        case "process_cloud_player_login": {
          // read the player
          var player = event.getContent().readObject(CloudPlayer.class);
          // push the change
          this.eventManager.callEvent(new BridgeProxyPlayerLoginEvent(player));
        }
        break;
        // player disconnection
        case "process_cloud_player_logout": {
          // read the player
          var player = event.getContent().readObject(CloudPlayer.class);
          // call the event locally
          this.eventManager.callEvent(new BridgeProxyPlayerDisconnectEvent(player));
        }
        break;
        // player service switch
        case "cloud_player_service_switch": {
          // read the information
          var player = event.getContent().readObject(CloudPlayer.class);
          var previous = event.getContent().readObject(NetworkServiceInfo.class);
          // fire the event
          this.eventManager.callEvent(new BridgeProxyPlayerServerSwitchEvent(player, previous));
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

  @EventListener
  public void handlePlayerExecutorChannelMessage(@NotNull ChannelMessageReceiveEvent event) {
    if (event.getChannel().equals(BridgeManagement.BRIDGE_PLAYER_EXECUTOR_CHANNEL_NAME) && event.getMessage() != null) {
      // the target unique id is always the first argument
      var executor = this.management.getDirectPlayerExecutor(event.getContent().readUniqueId());
      // a message regarding a player executor request
      switch (event.getMessage()) {
        // connect to a service
        case "connect_to_service":
          executor.connect(event.getContent().readString());
          break;
        // connect using the given selector
        case "connect_to_selector":
          executor.connectSelecting(event.getContent().readObject(ServerSelectorType.class));
          break;
        // connect to a fallback
        case "connect_to_fallback":
          executor.connectToFallback();
          break;
        // connect to a group
        case "connect_to_group":
          executor.connectToGroup(
            event.getContent().readString(),
            event.getContent().readObject(ServerSelectorType.class));
          break;
        // connect to a task
        case "connect_to_task":
          executor.connectToTask(
            event.getContent().readString(),
            event.getContent().readObject(ServerSelectorType.class));
          break;
        // kick a player from the current service
        case "kick_player":
          executor.kick(event.getContent().readObject(Component.class));
          break;
        // send a title to the player
        case "send_title":
          executor.sendTitle(event.getContent().readObject(Title.class));
          break;
        // send a chat message to the player
        case "send_chat_message":
          executor.sendChatMessage(
            event.getContent().readObject(Component.class),
            event.getContent().readNullable(DataBuf::readString));
          break;
        // send a plugin message to the player
        case "send_plugin_message":
          executor.sendPluginMessage(event.getContent().readString(), event.getContent().readByteArray());
          break;
        // sends a chat message like when a player does type into the chat
        case "dispatch_proxy_command":
          executor.dispatchProxyCommand(event.getContent().readString());
          break;
        // unable to handle
        default:
          break;
      }
    }
  }
}
