package de.dytanic.cloudnet.ext.bridge.node.player;

import de.dytanic.cloudnet.CloudNet;
import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.IDatabase;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.ext.bridge.BridgeConstants;
import de.dytanic.cloudnet.ext.bridge.player.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class NodePlayerManager implements IPlayerManager {

    private static NodePlayerManager instance;

    private final Map<UUID, CloudPlayer> onlineCloudPlayers = new ConcurrentHashMap<>();

    private final String databaseName;

    public NodePlayerManager(String databaseName) {
        this.databaseName = databaseName;

        instance = this;
    }

    public static NodePlayerManager getInstance() {
        return NodePlayerManager.instance;
    }


    public IDatabase getDatabase() {
        return CloudNet.getInstance().getDatabaseProvider().getDatabase(databaseName);
    }


    @Override
    public int getOnlineCount() {
        return this.onlineCloudPlayers.size();
    }

    @Override
    public long getRegisteredCount() {
        return this.getDatabase().getDocumentsCount();
    }

    @Nullable
    @Override
    public CloudPlayer getOnlinePlayer(@NotNull UUID uniqueId) {
        return onlineCloudPlayers.get(uniqueId);
    }

    @Override
    public List<? extends ICloudPlayer> getOnlinePlayers(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return this.onlineCloudPlayers.values().stream().filter(cloudPlayer -> cloudPlayer.getName().equalsIgnoreCase(name)).collect(Collectors.toList());
    }

    @Override
    public List<? extends ICloudPlayer> getOnlinePlayers(@NotNull ServiceEnvironmentType environment) {
        Preconditions.checkNotNull(environment);

        return this.onlineCloudPlayers.values().stream().filter(cloudPlayer -> (cloudPlayer.getLoginService() != null && cloudPlayer.getLoginService().getEnvironment() == environment) ||
                (cloudPlayer.getConnectedService() != null && cloudPlayer.getConnectedService().getEnvironment() == environment)).collect(Collectors.toList());
    }

    @Override
    public List<? extends ICloudPlayer> getOnlinePlayers() {
        return new ArrayList<>(this.onlineCloudPlayers.values());
    }

    @Override
    public ICloudOfflinePlayer getOfflinePlayer(@NotNull UUID uniqueId) {
        Preconditions.checkNotNull(uniqueId);

        JsonDocument jsonDocument = this.getDatabase().get(uniqueId.toString());

        return jsonDocument != null ? jsonDocument.toInstanceOf(CloudOfflinePlayer.TYPE) : null;
    }

    @Override
    public List<? extends ICloudOfflinePlayer> getOfflinePlayers(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return this.getDatabase().get(new JsonDocument("name", name)).stream().map(jsonDocument -> (CloudOfflinePlayer) jsonDocument.toInstanceOf(CloudOfflinePlayer.TYPE)).collect(Collectors.toList());
    }

    @Override
    @Deprecated
    public List<? extends ICloudOfflinePlayer> getRegisteredPlayers() {
        List<? extends ICloudOfflinePlayer> cloudOfflinePlayers = new ArrayList<>();

        this.getDatabase().iterate((s, jsonDocument) -> cloudOfflinePlayers.add(jsonDocument.toInstanceOf(CloudOfflinePlayer.TYPE)));

        return cloudOfflinePlayers;
    }

    @Override
    public ITask<Integer> getOnlineCountAsync() {
        return this.schedule(this::getOnlineCount);
    }

    @Override
    public ITask<Long> getRegisteredCountAsync() {
        return this.getDatabase().getDocumentsCountAsync();
    }

    @Override
    public ITask<ICloudPlayer> getOnlinePlayerAsync(@NotNull UUID uniqueId) {
        return this.schedule(() -> this.getOnlinePlayer(uniqueId));
    }

    @Override
    public ITask<List<? extends ICloudPlayer>> getOnlinePlayersAsync(@NotNull String name) {
        return this.schedule(() -> this.getOnlinePlayers(name));
    }

    @Override
    public ITask<List<? extends ICloudPlayer>> getOnlinePlayersAsync(@NotNull ServiceEnvironmentType environment) {
        return this.schedule(() -> this.getOnlinePlayers(environment));
    }

    @Override
    public ITask<List<? extends ICloudPlayer>> getOnlinePlayersAsync() {
        return this.schedule(this::getOnlinePlayers);
    }

    @Override
    public ITask<ICloudOfflinePlayer> getOfflinePlayerAsync(@NotNull UUID uniqueId) {
        return this.schedule(() -> this.getOfflinePlayer(uniqueId));
    }

    @Override
    public ITask<List<? extends ICloudOfflinePlayer>> getOfflinePlayersAsync(@NotNull String name) {
        return this.schedule(() -> this.getOfflinePlayers(name));
    }

    @Override
    public ITask<List<? extends ICloudOfflinePlayer>> getRegisteredPlayersAsync() {
        return this.schedule(this::getRegisteredPlayers);
    }

    @Override
    public void updateOfflinePlayer(@NotNull ICloudOfflinePlayer cloudOfflinePlayer) {
        Preconditions.checkNotNull(cloudOfflinePlayer);

        this.updateOfflinePlayer0(cloudOfflinePlayer);
        CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "update_offline_cloud_player",
                new JsonDocument(
                        "offlineCloudPlayer", cloudOfflinePlayer
                )
        );
    }

    public void updateOfflinePlayer0(ICloudOfflinePlayer cloudOfflinePlayer) {
        this.getDatabase().update(cloudOfflinePlayer.getUniqueId().toString(), JsonDocument.newDocument(cloudOfflinePlayer));
    }

    @Override
    public void updateOnlinePlayer(@NotNull ICloudPlayer cloudPlayer) {
        Preconditions.checkNotNull(cloudPlayer);

        this.updateOnlinePlayer0(cloudPlayer);
        CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "update_online_cloud_player",
                new JsonDocument(
                        "cloudPlayer", cloudPlayer
                )
        );
    }

    public void updateOnlinePlayer0(ICloudPlayer cloudPlayer) {
        if (this.onlineCloudPlayers.containsKey(cloudPlayer.getUniqueId())) {
            this.onlineCloudPlayers.put(cloudPlayer.getUniqueId(), (CloudPlayer) cloudPlayer);
        }
        this.updateOfflinePlayer0(CloudOfflinePlayer.of(cloudPlayer));
    }

    @Override
    public void proxySendPlayer(@NotNull ICloudPlayer cloudPlayer, @NotNull String serviceName) {
        Preconditions.checkNotNull(cloudPlayer);

        this.proxySendPlayer(cloudPlayer.getUniqueId(), serviceName);
    }

    @Override
    public void proxySendPlayer(@NotNull UUID uniqueId, @NotNull String serviceName) {
        Preconditions.checkNotNull(uniqueId);
        Preconditions.checkNotNull(serviceName);

        CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "send_on_proxy_player_to_server",
                new JsonDocument()
                        .append("uniqueId", uniqueId)
                        .append("serviceName", serviceName)
        );
    }

    @Override
    public void proxySendPlayerMessage(@NotNull ICloudPlayer cloudPlayer, @NotNull String message) {
        Preconditions.checkNotNull(cloudPlayer);

        this.proxySendPlayerMessage(cloudPlayer.getUniqueId(), message);
    }

    @Override
    public void proxySendPlayerMessage(@NotNull UUID uniqueId, @NotNull String message) {
        Preconditions.checkNotNull(uniqueId);
        Preconditions.checkNotNull(message);

        CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "send_message_to_proxy_player",
                new JsonDocument()
                        .append("uniqueId", uniqueId)
                        .append("message", message)
        );
    }

    @Override
    public void proxyKickPlayer(@NotNull ICloudPlayer cloudPlayer, @NotNull String kickMessage) {
        Preconditions.checkNotNull(cloudPlayer);

        this.proxyKickPlayer(cloudPlayer.getUniqueId(), kickMessage);
    }

    @Override
    public void proxyKickPlayer(@NotNull UUID uniqueId, @NotNull String kickMessage) {
        Preconditions.checkNotNull(uniqueId);
        Preconditions.checkNotNull(kickMessage);

        CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "kick_on_proxy_player_from_network",
                new JsonDocument()
                        .append("uniqueId", uniqueId)
                        .append("kickMessage", kickMessage)
        );
    }

    @Override
    public void broadcastMessage(@NotNull String message) {
        Preconditions.checkNotNull(message);

        CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
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

        CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "broadcast_message",
                new JsonDocument()
                        .append("message", message)
                        .append("permission", permission)
        );
    }


    private <T> ITask<T> schedule(Callable<T> callable) {
        return CloudNet.getInstance().getTaskScheduler().schedule(callable);
    }

    public Map<UUID, CloudPlayer> getOnlineCloudPlayers() {
        return this.onlineCloudPlayers;
    }

    public String getDatabaseName() {
        return this.databaseName;
    }
}
