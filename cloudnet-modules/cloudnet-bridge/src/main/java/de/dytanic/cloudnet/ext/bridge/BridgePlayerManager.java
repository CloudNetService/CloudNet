package de.dytanic.cloudnet.ext.bridge;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.ext.bridge.player.*;

import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public final class BridgePlayerManager implements IPlayerManager {

    private static final IPlayerManager instance = new BridgePlayerManager();

    private static final Type
            TYPE_LIST_CLOUD_PLAYERS = new TypeToken<List<CloudPlayer>>() {
    }.getType(),
            TYPE_LIST_CLOUD_OFFLINE_PLAYERS = new TypeToken<List<CloudOfflinePlayer>>() {
            }.getType();

    public static IPlayerManager getInstance() {
        return BridgePlayerManager.instance;
    }

    @Override
    public ICloudPlayer getOnlinePlayer(UUID uniqueId) {
        try {
            return getOnlinePlayerAsync(uniqueId).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public List<? extends ICloudPlayer> getOnlinePlayer(String name) {
        try {
            return getOnlinePlayerAsync(name).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }

        return Iterables.newArrayList();
    }

    @Override
    public List<? extends ICloudPlayer> getOnlinePlayers(ServiceEnvironmentType environment) {
        try {
            return getOnlinePlayersAsync(environment).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }

        return Iterables.newArrayList();
    }

    @Override
    public List<? extends ICloudPlayer> getOnlinePlayers() {
        try {
            return getOnlinePlayersAsync().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }

        return Iterables.newArrayList();
    }

    @Override
    public ICloudOfflinePlayer getOfflinePlayer(UUID uniqueId) {
        try {
            return getOfflinePlayerAsync(uniqueId).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public List<? extends ICloudOfflinePlayer> getOfflinePlayer(String name) {
        try {
            return getOfflinePlayerAsync(name).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }

        return Iterables.newArrayList();
    }

    @Override
    public List<? extends ICloudOfflinePlayer> getRegisteredPlayers() {
        try {
            return getRegisteredPlayersAsync().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }

        return Iterables.newArrayList();
    }


    @Override
    public ITask<? extends ICloudPlayer> getOnlinePlayerAsync(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        return getCloudNetDriver().sendCallablePacket(
                getCloudNetDriver().getNetworkClient().getChannels().iterator().next(),
                BridgeConstants.BRIDGE_CUSTOM_CALLABLE_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "get_online_players_by_uuid",
                new JsonDocument()
                        .append("uniqueId", uniqueId)
                ,
                (Function<JsonDocument, CloudPlayer>) jsonDocument -> jsonDocument.get("cloudPlayer", CloudPlayer.TYPE)
        );
    }

    @Override
    public ITask<List<? extends ICloudPlayer>> getOnlinePlayerAsync(String name) {
        Validate.checkNotNull(name);

        return getCloudNetDriver().sendCallablePacket(
                getCloudNetDriver().getNetworkClient().getChannels().iterator().next(),
                BridgeConstants.BRIDGE_CUSTOM_CALLABLE_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "get_online_players_by_name_as_list",
                new JsonDocument()
                        .append("name", name)
                ,
                jsonDocument -> jsonDocument.get("cloudPlayers", TYPE_LIST_CLOUD_PLAYERS)
        );
    }

    @Override
    public ITask<List<? extends ICloudPlayer>> getOnlinePlayersAsync(ServiceEnvironmentType environment) {
        Validate.checkNotNull(environment);

        return getCloudNetDriver().sendCallablePacket(
                getCloudNetDriver().getNetworkClient().getChannels().iterator().next(),
                BridgeConstants.BRIDGE_CUSTOM_CALLABLE_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "get_online_players_by_environment_as_list",
                new JsonDocument()
                        .append("environment", environment)
                ,
                jsonDocument -> jsonDocument.get("cloudPlayers", TYPE_LIST_CLOUD_PLAYERS)
        );
    }

    @Override
    public ITask<List<? extends ICloudPlayer>> getOnlinePlayersAsync() {
        return getCloudNetDriver().sendCallablePacket(
                getCloudNetDriver().getNetworkClient().getChannels().iterator().next(),
                BridgeConstants.BRIDGE_CUSTOM_CALLABLE_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "get_all_online_players_as_list",
                new JsonDocument(),
                jsonDocument -> jsonDocument.get("cloudPlayers", TYPE_LIST_CLOUD_PLAYERS)
        );
    }

    @Override
    public ITask<ICloudOfflinePlayer> getOfflinePlayerAsync(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        return getCloudNetDriver().sendCallablePacket(
                getCloudNetDriver().getNetworkClient().getChannels().iterator().next(),
                BridgeConstants.BRIDGE_CUSTOM_CALLABLE_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "get_offline_player_by_uuid",
                new JsonDocument()
                        .append("uniqueId", uniqueId)
                ,
                jsonDocument -> jsonDocument.get("offlineCloudPlayer", CloudOfflinePlayer.TYPE)
        );
    }

    @Override
    public ITask<List<? extends ICloudOfflinePlayer>> getOfflinePlayerAsync(String name) {
        Validate.checkNotNull(name);

        return getCloudNetDriver().sendCallablePacket(
                getCloudNetDriver().getNetworkClient().getChannels().iterator().next(),
                BridgeConstants.BRIDGE_CUSTOM_CALLABLE_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "get_offline_player_by_name_as_list",
                new JsonDocument()
                        .append("name", name)
                ,
                jsonDocument -> jsonDocument.get("offlineCloudPlayers", TYPE_LIST_CLOUD_OFFLINE_PLAYERS)
        );
    }

    @Override
    public ITask<List<? extends ICloudOfflinePlayer>> getRegisteredPlayersAsync() {
        return getCloudNetDriver().sendCallablePacket(
                getCloudNetDriver().getNetworkClient().getChannels().iterator().next(),
                BridgeConstants.BRIDGE_CUSTOM_CALLABLE_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "get_all_registered_offline_players_as_list",
                new JsonDocument(),
                jsonDocument -> jsonDocument.get("offlineCloudPlayers", TYPE_LIST_CLOUD_OFFLINE_PLAYERS)
        );
    }


    @Override
    public void updateOfflinePlayer(ICloudOfflinePlayer cloudOfflinePlayer) {
        Validate.checkNotNull(cloudOfflinePlayer);

        CloudNetDriver.getInstance().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "update_offline_cloud_player",
                new JsonDocument(
                        "offlineCloudPlayer", cloudOfflinePlayer
                )
        );
    }

    @Override
    public void updateOnlinePlayer(ICloudPlayer cloudPlayer) {
        Validate.checkNotNull(cloudPlayer);

        CloudNetDriver.getInstance().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "update_online_cloud_player",
                new JsonDocument(
                        "cloudPlayer", cloudPlayer
                )
        );
    }

    @Override
    public void proxySendPlayer(ICloudPlayer cloudPlayer, String serviceName) {
        Validate.checkNotNull(cloudPlayer);
        Validate.checkNotNull(serviceName);

        getCloudNetDriver().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "send_on_proxy_player_to_server",
                new JsonDocument()
                        .append("uniqueId", cloudPlayer.getUniqueId())
                        .append("name", cloudPlayer.getName())
                        .append("serviceName", serviceName)
        );
    }

    @Override
    public void proxyKickPlayer(ICloudPlayer cloudPlayer, String kickMessage) {
        Validate.checkNotNull(cloudPlayer);
        Validate.checkNotNull(kickMessage);

        getCloudNetDriver().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "kick_on_proxy_player_from_network",
                new JsonDocument()
                        .append("uniqueId", cloudPlayer.getUniqueId())
                        .append("name", cloudPlayer.getName())
                        .append("kickMessage", kickMessage)
        );
    }

    @Override
    public void proxySendPlayerMessage(ICloudPlayer cloudPlayer, String message) {
        Validate.checkNotNull(cloudPlayer);
        Validate.checkNotNull(message);

        getCloudNetDriver().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "send_message_to_proxy_player",
                new JsonDocument()
                        .append("uniqueId", cloudPlayer.getUniqueId())
                        .append("name", cloudPlayer.getName())
                        .append("message", message)
        );
    }

    @Override
    public void broadcastMessage(String message) {
        Validate.checkNotNull(message);

        getCloudNetDriver().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "broadcast_message",
                new JsonDocument()
                        .append("message", message)
        );
    }

    @Override
    public void broadcastMessage(String message, String permission) {
        Validate.checkNotNull(message);
        Validate.checkNotNull(permission);

        getCloudNetDriver().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "broadcast_message",
                new JsonDocument()
                        .append("message", message)
                        .append("permission", permission)
        );
    }


    private CloudNetDriver getCloudNetDriver() {
        return CloudNetDriver.getInstance();
    }
}