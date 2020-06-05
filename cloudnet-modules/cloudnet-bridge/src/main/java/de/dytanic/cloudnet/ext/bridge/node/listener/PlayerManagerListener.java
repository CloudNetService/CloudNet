package de.dytanic.cloudnet.ext.bridge.node.listener;

import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceStopEvent;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.event.cluster.NetworkChannelAuthClusterNodeSuccessEvent;
import de.dytanic.cloudnet.ext.bridge.BridgeConstants;
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
        for (ICloudPlayer cloudPlayer : this.nodePlayerManager.getOnlineCloudPlayers().values()) {
            if (cloudPlayer.getLoginService() != null && cloudPlayer.getLoginService().getUniqueId().equals(event.getServiceInfo().getServiceId().getUniqueId())) {
                this.nodePlayerManager.getOnlineCloudPlayers().remove(cloudPlayer.getUniqueId());
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
        if (!event.getChannel().equalsIgnoreCase(BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL) || event.getMessage() == null) {
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

                this.nodePlayerManager.updateOfflinePlayer0(cloudOfflinePlayer);
            }
            break;
            case "update_online_cloud_player": {
                ICloudPlayer cloudPlayer = event.getBuffer().readObject(CloudPlayer.class);

                this.nodePlayerManager.updateOnlinePlayer0(cloudPlayer);
            }
            break;
        }
    }

    @EventListener
    public void handleQuery(ChannelMessageReceiveEvent event) {
        if (!event.getChannel().equals(BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL) || !event.isQuery() || event.getMessage() == null) {
            return;
        }

        switch (event.getMessage().toLowerCase()) {
            case "get_online_count": {
                event.setQueryResponse(ChannelMessage.buildResponseFor(event.getChannelMessage())
                        .buffer(ProtocolBuffer.create().writeInt(this.nodePlayerManager.getOnlineCount()))
                        .build());
            }
            break;
            case "get_registered_count": {
                event.setQueryResponse(ChannelMessage.buildResponseFor(event.getChannelMessage())
                        .buffer(ProtocolBuffer.create().writeLong(this.nodePlayerManager.getRegisteredCount()))
                        .build());
            }
            break;
            case "get_online_player_by_uuid": {
                UUID uniqueId = event.getBuffer().readUUID();
                event.setQueryResponse(ChannelMessage.buildResponseFor(event.getChannelMessage())
                        .buffer(ProtocolBuffer.create().writeOptionalObject(this.nodePlayerManager.getOnlinePlayer(uniqueId)))
                        .build());
            }
            break;
            case "get_online_players_by_name": {
                String name = event.getBuffer().readString();
                event.setQueryResponse(ChannelMessage.buildResponseFor(event.getChannelMessage())
                        .buffer(ProtocolBuffer.create().writeObjectCollection(this.nodePlayerManager.getOnlinePlayers(name)))
                        .build());
            }
            break;
            case "get_online_players_by_environment": {
                ServiceEnvironmentType environment = event.getBuffer().readEnumConstant(ServiceEnvironmentType.class);
                event.setQueryResponse(ChannelMessage.buildResponseFor(event.getChannelMessage())
                        .buffer(ProtocolBuffer.create().writeObjectCollection(this.nodePlayerManager.getOnlinePlayers(environment)))
                        .build());
            }
            break;
            case "get_online_players": {
                event.setQueryResponse(ChannelMessage.buildResponseFor(event.getChannelMessage())
                        .buffer(ProtocolBuffer.create().writeObjectCollection(this.nodePlayerManager.getOnlinePlayers()))
                        .build());
            }
            break;
            case "get_offline_player_by_uuid": {
                UUID uniqueId = event.getBuffer().readUUID();
                event.setQueryResponse(ChannelMessage.buildResponseFor(event.getChannelMessage())
                        .buffer(ProtocolBuffer.create().writeOptionalObject(this.nodePlayerManager.getOfflinePlayer(uniqueId)))
                        .build());
            }
            break;
            case "get_offline_players_by_name": {
                String name = event.getBuffer().readString();
                event.setQueryResponse(ChannelMessage.buildResponseFor(event.getChannelMessage())
                        .buffer(ProtocolBuffer.create().writeObjectCollection(this.nodePlayerManager.getOfflinePlayers(name)))
                        .build());
            }
            break;
            case "get_offline_players": {
                event.setQueryResponse(ChannelMessage.buildResponseFor(event.getChannelMessage())
                        .buffer(ProtocolBuffer.create().writeObjectCollection(this.nodePlayerManager.getRegisteredPlayers()))
                        .build());
            }
            break;
        }
    }
}
