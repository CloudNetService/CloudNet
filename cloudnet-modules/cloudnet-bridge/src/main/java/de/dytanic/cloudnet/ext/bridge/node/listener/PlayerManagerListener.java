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

package de.dytanic.cloudnet.ext.bridge.node.listener;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceStopEvent;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.event.cluster.NetworkChannelAuthClusterNodeSuccessEvent;
import de.dytanic.cloudnet.ext.bridge.BridgeConstants;
import de.dytanic.cloudnet.ext.bridge.event.BridgeDeleteCloudOfflinePlayerEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeUpdateCloudOfflinePlayerEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeUpdateCloudPlayerEvent;
import de.dytanic.cloudnet.ext.bridge.node.player.NodePlayerManager;
import de.dytanic.cloudnet.ext.bridge.player.CloudOfflinePlayer;
import de.dytanic.cloudnet.ext.bridge.player.CloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.ICloudOfflinePlayer;
import de.dytanic.cloudnet.ext.bridge.player.ICloudPlayer;
import java.util.Collection;
import java.util.UUID;

public final class PlayerManagerListener {

  private final NodePlayerManager nodePlayerManager;

  public PlayerManagerListener(NodePlayerManager nodePlayerManager) {
    this.nodePlayerManager = nodePlayerManager;
  }

  @EventListener
  public void handle(CloudServiceStopEvent event) {
    UUID serviceId = event.getServiceInfo().getServiceId().getUniqueId();
    for (CloudPlayer cloudPlayer : this.nodePlayerManager.getOnlineCloudPlayers().values()) {
      if (cloudPlayer.getLoginService() == null) {
        continue;
      }

      if (cloudPlayer.getLoginService().getEnvironment().isMinecraftProxy()) {
        if (cloudPlayer.getLoginService().getUniqueId().equals(serviceId)) {
          this.nodePlayerManager.logoutPlayer(cloudPlayer);
        }
        continue;
      }

      UUID connectedServiceId = cloudPlayer.getLoginService().getUniqueId();
      if (cloudPlayer.getConnectedService() != null
        && connectedServiceId.equals(serviceId)
        && cloudPlayer.getLoginService().getServiceId().getUniqueId().equals(connectedServiceId)) {
        this.nodePlayerManager.logoutPlayer(cloudPlayer);
      }
    }
  }

  @EventListener
  public void handle(NetworkChannelAuthClusterNodeSuccessEvent event) {
    event.getNode().sendCustomChannelMessage(ChannelMessage.builder()
      .channel(BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL)
      .message("set_online_players")
      .buffer(ProtocolBuffer.create().writeObjectCollection(this.nodePlayerManager.getOnlineCloudPlayers().values()))
      .targetNode(event.getNode().getNodeInfo().getUniqueId())
      .build());
  }

  @EventListener
  public void handle(ChannelMessageReceiveEvent event) {
    if (!event.getChannel().equalsIgnoreCase(BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL)
      || event.getMessage() == null) {
      return;
    }

    switch (event.getMessage().toLowerCase()) {
      case "set_online_players": {
        Collection<CloudPlayer> cloudPlayers = event.getBuffer().readObjectCollection(CloudPlayer.class);

        for (CloudPlayer cloudPlayer : cloudPlayers) {
          this.nodePlayerManager.getOnlineCloudPlayers().put(cloudPlayer.getUniqueId(), cloudPlayer);
        }
      }
      break;
      case "update_offline_cloud_player": {
        ICloudOfflinePlayer cloudOfflinePlayer = event.getBuffer().readObject(CloudOfflinePlayer.class);

        this.nodePlayerManager.handleOfflinePlayerUpdate(cloudOfflinePlayer);
        CloudNetDriver.getInstance().getEventManager()
          .callEvent(new BridgeUpdateCloudOfflinePlayerEvent(cloudOfflinePlayer));
      }
      break;
      case "update_online_cloud_player": {
        ICloudPlayer cloudPlayer = event.getBuffer().readObject(CloudPlayer.class);

        this.nodePlayerManager.handleOnlinePlayerUpdate(cloudPlayer);
        CloudNetDriver.getInstance().getEventManager().callEvent(new BridgeUpdateCloudPlayerEvent(cloudPlayer));
      }
      break;
      case "process_cloud_player_login": {
        CloudPlayer cloudPlayer = event.getBuffer().readObject(CloudPlayer.class);
        this.nodePlayerManager.processLoginMessage(cloudPlayer);
      }
      break;
      case "delete_offline_player": {
        ICloudOfflinePlayer cloudOfflinePlayer = event.getBuffer().readObject(CloudOfflinePlayer.class);

        this.nodePlayerManager.deleteCloudOfflinePlayer0(cloudOfflinePlayer);
        CloudNetDriver.getInstance().getEventManager()
          .callEvent(new BridgeDeleteCloudOfflinePlayerEvent(cloudOfflinePlayer));
      }
      break;
      default:
        break;
    }
  }

  @EventListener
  public void handleQuery(ChannelMessageReceiveEvent event) {
    if (!event.getChannel().equals(BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL) || !event.isQuery()
      || event.getMessage() == null) {
      return;
    }

    switch (event.getMessage().toLowerCase()) {
      case "get_online_count": {
        event.createBinaryResponse().writeInt(this.nodePlayerManager.getOnlineCount());
      }
      break;
      case "get_registered_count": {
        event.createBinaryResponse().writeLong(this.nodePlayerManager.getRegisteredCount());
      }
      break;
      case "get_online_player_by_uuid": {
        UUID uniqueId = event.getBuffer().readUUID();
        event.createBinaryResponse().writeOptionalObject(this.nodePlayerManager.getOnlinePlayer(uniqueId));
      }
      break;
      case "get_online_players_by_name": {
        String name = event.getBuffer().readString();
        event.createBinaryResponse().writeObjectCollection(this.nodePlayerManager.getOnlinePlayers(name));
      }
      break;
      case "get_online_players_by_environment": {
        ServiceEnvironmentType environment = event.getBuffer().readEnumConstant(ServiceEnvironmentType.class);
        event.createBinaryResponse().writeObjectCollection(this.nodePlayerManager.getOnlinePlayers(environment));
      }
      break;
      case "online_players_player": {
        event.createBinaryResponse().writeObjectCollection(this.nodePlayerManager.onlinePlayers().asPlayers());
      }
      break;
      case "online_players_uuid": {
        event.createBinaryResponse().writeUUIDCollection(this.nodePlayerManager.onlinePlayers().asUUIDs());
      }
      break;
      case "online_players_name": {
        event.createBinaryResponse().writeStringCollection(this.nodePlayerManager.onlinePlayers().asNames());
      }
      break;
      case "online_players_count": {
        event.createBinaryResponse().writeVarInt(this.nodePlayerManager.onlinePlayers().count());
      }
      break;
      case "online_players_task_player": {
        String task = event.getBuffer().readString();
        event.createBinaryResponse().writeObjectCollection(this.nodePlayerManager.taskOnlinePlayers(task).asPlayers());
      }
      break;
      case "online_players_task_uuid": {
        String task = event.getBuffer().readString();
        event.createBinaryResponse().writeUUIDCollection(this.nodePlayerManager.taskOnlinePlayers(task).asUUIDs());
      }
      break;
      case "online_players_task_name": {
        String task = event.getBuffer().readString();
        event.createBinaryResponse().writeStringCollection(this.nodePlayerManager.taskOnlinePlayers(task).asNames());
      }
      break;
      case "online_players_task_count": {
        String task = event.getBuffer().readString();
        event.createBinaryResponse().writeVarInt(this.nodePlayerManager.taskOnlinePlayers(task).count());
      }
      break;
      case "online_players_group_player": {
        String group = event.getBuffer().readString();
        event.createBinaryResponse()
          .writeObjectCollection(this.nodePlayerManager.groupOnlinePlayers(group).asPlayers());
      }
      break;
      case "online_players_group_uuid": {
        String group = event.getBuffer().readString();
        event.createBinaryResponse().writeUUIDCollection(this.nodePlayerManager.groupOnlinePlayers(group).asUUIDs());
      }
      break;
      case "online_players_group_name": {
        String group = event.getBuffer().readString();
        event.createBinaryResponse().writeStringCollection(this.nodePlayerManager.groupOnlinePlayers(group).asNames());
      }
      break;
      case "online_players_group_count": {
        String group = event.getBuffer().readString();
        event.createBinaryResponse().writeVarInt(this.nodePlayerManager.groupOnlinePlayers(group).count());
      }
      break;
      case "get_offline_player_by_uuid": {
        UUID uniqueId = event.getBuffer().readUUID();
        event.createBinaryResponse().writeOptionalObject(this.nodePlayerManager.getOfflinePlayer(uniqueId));
      }
      break;
      case "get_offline_players_by_name": {
        String name = event.getBuffer().readString();
        event.createBinaryResponse().writeObjectCollection(this.nodePlayerManager.getOfflinePlayers(name));
      }
      break;
      case "get_offline_players": {
        event.createBinaryResponse().writeObjectCollection(this.nodePlayerManager.getRegisteredPlayers());
      }
      break;
      default:
        break;
    }
  }
}
