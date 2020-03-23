package de.dytanic.cloudnet.ext.bridge;

import com.google.common.base.Preconditions;
import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.ext.bridge.player.*;
import de.dytanic.cloudnet.ext.bridge.player.executor.DefaultPlayerExecutor;
import de.dytanic.cloudnet.ext.bridge.player.executor.PlayerExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.ArrayList;
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
    public int getOnlineCount() {
        return this.getOnlineCountAsync().get(5, TimeUnit.SECONDS, -1);
    }

    @Override
    public long getRegisteredCount() {
        return this.getRegisteredCountAsync().get(5, TimeUnit.SECONDS, -1L);
    }

    @Nullable
    @Override
    public ICloudPlayer getOnlinePlayer(@NotNull UUID uniqueId) {
        try {
            return this.getOnlinePlayerAsync(uniqueId).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }

        return null;
    }

    @Override
    public List<? extends ICloudPlayer> getOnlinePlayers(@NotNull String name) {
        try {
            return this.getOnlinePlayersAsync(name).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }

        return new ArrayList<>();
    }

    @Override
    public List<? extends ICloudPlayer> getOnlinePlayers(@NotNull ServiceEnvironmentType environment) {
        try {
            return this.getOnlinePlayersAsync(environment).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }

        return new ArrayList<>();
    }

    @Override
    public List<? extends ICloudPlayer> getOnlinePlayers() {
        try {
            return this.getOnlinePlayersAsync().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }

        return new ArrayList<>();
    }

    @Override
    public ICloudOfflinePlayer getOfflinePlayer(@NotNull UUID uniqueId) {
        try {
            return this.getOfflinePlayerAsync(uniqueId).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }

        return null;
    }

    @Override
    public List<? extends ICloudOfflinePlayer> getOfflinePlayers(@NotNull String name) {
        try {
            return this.getOfflinePlayersAsync(name).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }

        return new ArrayList<>();
    }

    @Override
    public List<? extends ICloudOfflinePlayer> getRegisteredPlayers() {
        try {
            return this.getRegisteredPlayersAsync().get(5, TimeUnit.MINUTES);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }

        return new ArrayList<>();
    }

    @Override
    @NotNull
    public ITask<Integer> getOnlineCountAsync() {
        return this.getCloudNetDriver().getPacketQueryProvider().sendCallablePacket(
                this.getCloudNetDriver().getNetworkClient().getChannels().iterator().next(),
                BridgeConstants.BRIDGE_CUSTOM_CALLABLE_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "get_online_count",
                new JsonDocument(),
                jsonDocument -> jsonDocument.getInt("onlineCount")
        );
    }

    @Override
    @NotNull
    public ITask<Long> getRegisteredCountAsync() {
        return this.getCloudNetDriver().getPacketQueryProvider().sendCallablePacket(
                this.getCloudNetDriver().getNetworkClient().getChannels().iterator().next(),
                BridgeConstants.BRIDGE_CUSTOM_CALLABLE_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "get_registered_count",
                new JsonDocument(),
                jsonDocument -> jsonDocument.getLong("registeredCount")
        );
    }


    @Override
    @NotNull
    public ITask<? extends ICloudPlayer> getOnlinePlayerAsync(@NotNull UUID uniqueId) {
        Preconditions.checkNotNull(uniqueId);

        return this.getCloudNetDriver().getPacketQueryProvider().sendCallablePacket(
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
    @NotNull
    public ITask<List<? extends ICloudPlayer>> getOnlinePlayersAsync(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return this.getCloudNetDriver().getPacketQueryProvider().sendCallablePacket(
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
    @NotNull
    public ITask<List<? extends ICloudPlayer>> getOnlinePlayersAsync(@NotNull ServiceEnvironmentType environment) {
        Preconditions.checkNotNull(environment);

        return this.getCloudNetDriver().getPacketQueryProvider().sendCallablePacket(
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
    @NotNull
    public ITask<List<? extends ICloudPlayer>> getOnlinePlayersAsync() {
        return this.getCloudNetDriver().getPacketQueryProvider().sendCallablePacket(
                getCloudNetDriver().getNetworkClient().getChannels().iterator().next(),
                BridgeConstants.BRIDGE_CUSTOM_CALLABLE_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "get_all_online_players_as_list",
                new JsonDocument(),
                jsonDocument -> jsonDocument.get("cloudPlayers", TYPE_LIST_CLOUD_PLAYERS)
        );
    }

    @Override
    @NotNull
    public ITask<ICloudOfflinePlayer> getOfflinePlayerAsync(@NotNull UUID uniqueId) {
        Preconditions.checkNotNull(uniqueId);

        return this.getCloudNetDriver().getPacketQueryProvider().sendCallablePacket(
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
    @NotNull
    public ITask<List<? extends ICloudOfflinePlayer>> getOfflinePlayersAsync(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return this.getCloudNetDriver().getPacketQueryProvider().sendCallablePacket(
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
    @NotNull
    public ITask<List<? extends ICloudOfflinePlayer>> getRegisteredPlayersAsync() {
        return this.getCloudNetDriver().getPacketQueryProvider().sendCallablePacket(
                getCloudNetDriver().getNetworkClient().getChannels().iterator().next(),
                BridgeConstants.BRIDGE_CUSTOM_CALLABLE_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "get_all_registered_offline_players_as_list",
                new JsonDocument(),
                jsonDocument -> jsonDocument.get("offlineCloudPlayers", TYPE_LIST_CLOUD_OFFLINE_PLAYERS)
        );
    }


    @Override
    public void updateOfflinePlayer(@NotNull ICloudOfflinePlayer cloudOfflinePlayer) {
        Preconditions.checkNotNull(cloudOfflinePlayer);

        CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "update_offline_cloud_player",
                new JsonDocument("offlineCloudPlayer", cloudOfflinePlayer)
        );
    }

    @Override
    public void updateOnlinePlayer(@NotNull ICloudPlayer cloudPlayer) {
        Preconditions.checkNotNull(cloudPlayer);

        CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "update_online_cloud_player",
                new JsonDocument("cloudPlayer", cloudPlayer)
        );
    }

    @Override
    public @NotNull PlayerExecutor getPlayerExecutor(@NotNull UUID uniqueId) {
        return new DefaultPlayerExecutor(uniqueId);
    }

    @Override
    public void broadcastMessage(@NotNull String message) {
        Preconditions.checkNotNull(message);

        getCloudNetDriver().getMessenger().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "broadcast_message",
                new JsonDocument()
                        .append("message", message)
        );
    }

    @Override
    public void broadcastMessage(@NotNull String message, @NotNull String permission) {
        Preconditions.checkNotNull(message);
        Preconditions.checkNotNull(permission);

        getCloudNetDriver().getMessenger().sendChannelMessage(
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