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

    @EventListener
    public void handle(CloudServiceStopEvent event) {
        for (ICloudPlayer cloudPlayer : NodePlayerManager.getInstance().getOnlineCloudPlayers().values()) {
            if (cloudPlayer.getLoginService() != null && cloudPlayer.getLoginService().getUniqueId().equals(event.getServiceInfo().getServiceId().getUniqueId())) {
                NodePlayerManager.getInstance().getOnlineCloudPlayers().remove(cloudPlayer.getUniqueId());
            }
        }
    }

    @EventListener
    public void handle(NetworkChannelAuthClusterNodeSuccessEvent event) {
        event.getNode().sendCustomChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "send_all_online_players",
                new JsonDocument("cloudPlayers", NodePlayerManager.getInstance().getOnlineCloudPlayers().values())
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
                        NodePlayerManager.getInstance().getOnlineCloudPlayers().put(cloudPlayer.getUniqueId(), cloudPlayer);
                    }
                }
            }
            break;
            case "update_offline_cloud_player": {
                ICloudOfflinePlayer cloudOfflinePlayer = event.getData().get("offlineCloudPlayer", CloudOfflinePlayer.TYPE);

                if (cloudOfflinePlayer != null) { //todo shouldn't we call the BridgeUpdateCloudOfflinePlayerEvent here? and if this message comes from a service (and not from a node), call updateOfflinePlayer instead of updateOfflinePlayer0
                    NodePlayerManager.getInstance().updateOfflinePlayer0(cloudOfflinePlayer);
                }
            }
            break;
            case "update_online_cloud_player": {
                ICloudPlayer cloudPlayer = event.getData().get("cloudPlayer", CloudPlayer.TYPE);

                if (cloudPlayer != null) { //todo shouldn't we call the BridgeUpdateCloudPlayerEvent here? and if this message comes from a service (and not from a node), call updateOnlinePlayer instead of updateOnlinePlayer0
                    NodePlayerManager.getInstance().updateOnlinePlayer0(cloudPlayer);
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
                        .append("onlineCount", NodePlayerManager.getInstance().getOnlineCount())
                );
            }
            break;
            case "get_registered_count": {
                event.setCallbackPacket(new JsonDocument()
                        .append("registeredCount", NodePlayerManager.getInstance().getRegisteredCount())
                );
            }
            break;
            case "get_online_players_by_uuid": {
                event.setCallbackPacket(new JsonDocument()
                        .append("cloudPlayer", NodePlayerManager.getInstance().getOnlinePlayer(event.getHeader().get("uniqueId", UUID.class)))
                );
            }
            break;
            case "get_online_players_by_name_as_list": {
                event.setCallbackPacket(new JsonDocument()
                        .append("cloudPlayers", NodePlayerManager.getInstance().getOnlinePlayer(event.getHeader().getString("name")))
                );
            }
            break;
            case "get_online_players_by_environment_as_list": {
                event.setCallbackPacket(new JsonDocument()
                        .append("cloudPlayers", NodePlayerManager.getInstance().getOnlinePlayers(event.getHeader().get("environment", ServiceEnvironmentType.class)))
                );
            }
            break;
            case "get_all_online_players_as_list": {
                event.setCallbackPacket(new JsonDocument()
                        .append("cloudPlayers", NodePlayerManager.getInstance().getOnlinePlayers())
                );
            }
            break;
            case "get_offline_player_by_uuid": {
                event.setCallbackPacket(new JsonDocument()
                        .append("offlineCloudPlayer", NodePlayerManager.getInstance().getOfflinePlayer(event.getHeader().get("uniqueId", UUID.class)))
                );
            }
            break;
            case "get_offline_player_by_name_as_list": {
                event.setCallbackPacket(new JsonDocument()
                        .append("offlineCloudPlayers", NodePlayerManager.getInstance().getOfflinePlayer(event.getHeader().getString("name")))
                );
            }
            break;
            case "get_all_registered_offline_players_as_list": {
                event.setCallbackPacket(new JsonDocument()
                        .append("offlineCloudPlayers", NodePlayerManager.getInstance().getRegisteredPlayers())
                );
            }
            break;
            case "get_registered_offline_players_chunk": {
                event.setCallbackPacket(new JsonDocument()
                        .append("offlineCloudPlayers", NodePlayerManager.getInstance().getRegisteredPlayersInRange(event.getHeader().getInt("from"), event.getHeader().getInt("to")))
                );
            }
            break;
        }
    }
}