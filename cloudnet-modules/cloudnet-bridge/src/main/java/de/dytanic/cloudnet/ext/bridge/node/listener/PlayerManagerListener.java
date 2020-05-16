package de.dytanic.cloudnet.ext.bridge.node.listener;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceStopEvent;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.event.cluster.NetworkChannelAuthClusterNodeSuccessEvent;
import de.dytanic.cloudnet.event.network.NetworkChannelReceiveCallablePacketEvent;
import de.dytanic.cloudnet.ext.bridge.BridgeConstants;
import de.dytanic.cloudnet.ext.bridge.node.player.NodePlayerManager;
import de.dytanic.cloudnet.ext.bridge.player.CloudOfflinePlayer;
import de.dytanic.cloudnet.ext.bridge.player.CloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.ICloudOfflinePlayer;
import de.dytanic.cloudnet.ext.bridge.player.ICloudPlayer;

import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;

public final class PlayerManagerListener {

    private static final Type TYPE_CLOUD_PLAYERS_LIST = new TypeToken<List<CloudPlayer>>() {
    }.getType();

    private final NodePlayerManager nodePlayerManager;

    public PlayerManagerListener(NodePlayerManager nodePlayerManager) {
        this.nodePlayerManager = nodePlayerManager;
    }

    @EventListener
    public void handle(CloudServiceStopEvent event) {
        for (ICloudPlayer cloudPlayer : this.nodePlayerManager.getOnlineCloudPlayers().values()) {
            if (cloudPlayer.getLoginService() != null && cloudPlayer.getLoginService().getUniqueId().equals(event.getServiceInfo().getServiceId().getUniqueId())) {
                this.nodePlayerManager.getOnlineCloudPlayers().remove(cloudPlayer.getUniqueId());
            }
        }
    }

    @EventListener
    public void handle(NetworkChannelAuthClusterNodeSuccessEvent event) {
        event.getNode().sendCustomChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "send_all_online_players",
                new JsonDocument("cloudPlayers", this.nodePlayerManager.getOnlineCloudPlayers().values())
        );
    }

    @EventListener
    public void handle(ChannelMessageReceiveEvent event) {
        if (!event.getChannel().equalsIgnoreCase(BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME)) {
            return;
        }

        switch (event.getMessage().toLowerCase()) {
            case "send_all_online_players": {
                List<CloudPlayer> cloudPlayers = event.getData().get("cloudPlayers", TYPE_CLOUD_PLAYERS_LIST);

                if (cloudPlayers != null) {
                    for (CloudPlayer cloudPlayer : cloudPlayers) {
                        this.nodePlayerManager.getOnlineCloudPlayers().put(cloudPlayer.getUniqueId(), cloudPlayer);
                    }
                }
            }
            break;
            case "update_offline_cloud_player": {
                ICloudOfflinePlayer cloudOfflinePlayer = event.getData().get("offlineCloudPlayer", CloudOfflinePlayer.TYPE);

                if (cloudOfflinePlayer != null) {
                    this.nodePlayerManager.updateOfflinePlayer0(cloudOfflinePlayer);
                }
            }
            break;
            case "update_online_cloud_player": {
                ICloudPlayer cloudPlayer = event.getData().get("cloudPlayer", CloudPlayer.TYPE);

                if (cloudPlayer != null) {
                    this.nodePlayerManager.updateOnlinePlayer0(cloudPlayer);
                }
            }
            break;
        }
    }

    @EventListener
    public void handle(NetworkChannelReceiveCallablePacketEvent event) {
        if (!event.getChannelName().equalsIgnoreCase(BridgeConstants.BRIDGE_CUSTOM_CALLABLE_CHANNEL_PLAYER_API_CHANNEL_NAME)) {
            return;
        }

        switch (event.getId().toLowerCase()) {
            case "get_online_count": {
                event.setCallbackPacket(new JsonDocument()
                        .append("onlineCount", this.nodePlayerManager.getOnlineCount())
                );
            }
            break;
            case "get_registered_count": {
                event.setCallbackPacket(new JsonDocument()
                        .append("registeredCount", this.nodePlayerManager.getRegisteredCount())
                );
            }
            break;
            case "get_online_players_by_uuid": {
                event.setCallbackPacket(new JsonDocument()
                        .append("cloudPlayer", this.nodePlayerManager.getOnlinePlayer(event.getHeader().get("uniqueId", UUID.class)))
                );
            }
            break;
            case "get_online_players_by_name_as_list": {
                event.setCallbackPacket(new JsonDocument()
                        .append("cloudPlayers", this.nodePlayerManager.getOnlinePlayers(event.getHeader().getString("name")))
                );
            }
            break;
            case "get_online_players_by_environment_as_list": {
                event.setCallbackPacket(new JsonDocument()
                        .append("cloudPlayers", this.nodePlayerManager.getOnlinePlayers(event.getHeader().get("environment", ServiceEnvironmentType.class)))
                );
            }
            break;
            case "get_all_online_players_as_list": {
                event.setCallbackPacket(new JsonDocument()
                        .append("cloudPlayers", this.nodePlayerManager.getOnlinePlayers())
                );
            }
            break;
            case "get_offline_player_by_uuid": {
                event.setCallbackPacket(new JsonDocument()
                        .append("offlineCloudPlayer", this.nodePlayerManager.getOfflinePlayer(event.getHeader().get("uniqueId", UUID.class)))
                );
            }
            break;
            case "get_offline_player_by_name_as_list": {
                event.setCallbackPacket(new JsonDocument()
                        .append("offlineCloudPlayers", this.nodePlayerManager.getOfflinePlayers(event.getHeader().getString("name")))
                );
            }
            break;
            case "get_all_registered_offline_players_as_list": {
                event.setCallbackPacket(new JsonDocument()
                        .append("offlineCloudPlayers", this.nodePlayerManager.getRegisteredPlayers())
                );
            }
            break;
        }
    }
}
