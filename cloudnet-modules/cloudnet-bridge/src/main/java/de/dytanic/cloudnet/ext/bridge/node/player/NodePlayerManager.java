package de.dytanic.cloudnet.ext.bridge.node.player;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.collection.Maps;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.IDatabase;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.ext.bridge.BridgeConstants;
import de.dytanic.cloudnet.ext.bridge.player.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

public final class NodePlayerManager implements IPlayerManager {

    private static NodePlayerManager instance;

    private final Map<UUID, CloudPlayer> onlineCloudPlayers = Maps.newConcurrentHashMap();

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
    public CloudPlayer getOnlinePlayer(UUID uniqueId) {
        return onlineCloudPlayers.get(uniqueId);
    }

    @Override
    public List<? extends ICloudPlayer> getOnlinePlayer(String name) {
        Validate.checkNotNull(name);

        return Iterables.filter(onlineCloudPlayers.values(), cloudPlayer -> cloudPlayer.getName().equalsIgnoreCase(name));
    }

    @Override
    public List<? extends ICloudPlayer> getOnlinePlayers(ServiceEnvironmentType environment) {
        Validate.checkNotNull(environment);

        return Iterables.filter(onlineCloudPlayers.values(), cloudPlayer -> (cloudPlayer.getLoginService() != null && cloudPlayer.getLoginService().getEnvironment() == environment) ||
                (cloudPlayer.getConnectedService() != null && cloudPlayer.getConnectedService().getEnvironment() == environment));
    }

    @Override
    public List<? extends ICloudPlayer> getOnlinePlayers() {
        return Iterables.newArrayList(onlineCloudPlayers.values());
    }

    @Override
    public ICloudOfflinePlayer getOfflinePlayer(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        JsonDocument jsonDocument = getDatabase().get(uniqueId.toString());

        return jsonDocument != null ? jsonDocument.toInstanceOf(CloudOfflinePlayer.TYPE) : null;
    }

    @Override
    public List<? extends ICloudOfflinePlayer> getOfflinePlayer(String name) {
        Validate.checkNotNull(name);

        return Iterables.map(getDatabase().get(new JsonDocument("name", name)), jsonDocument -> jsonDocument.toInstanceOf(CloudOfflinePlayer.TYPE));
    }

    @Override
    public List<? extends ICloudOfflinePlayer> getRegisteredPlayers() {
        List<? extends ICloudOfflinePlayer> cloudOfflinePlayers = Iterables.newArrayList();

        getDatabase().iterate((s, jsonDocument) -> cloudOfflinePlayers.add(jsonDocument.toInstanceOf(CloudOfflinePlayer.TYPE)));

        return cloudOfflinePlayers;
    }


    @Override
    public ITask<ICloudPlayer> getOnlinePlayerAsync(UUID uniqueId) {
        return schedule(() -> getOnlinePlayer(uniqueId));
    }

    @Override
    public ITask<List<? extends ICloudPlayer>> getOnlinePlayerAsync(String name) {
        return schedule(() -> getOnlinePlayer(name));
    }

    @Override
    public ITask<List<? extends ICloudPlayer>> getOnlinePlayersAsync(ServiceEnvironmentType environment) {
        return schedule(() -> getOnlinePlayers(environment));
    }

    @Override
    public ITask<List<? extends ICloudPlayer>> getOnlinePlayersAsync() {
        return schedule(this::getOnlinePlayers);
    }

    @Override
    public ITask<ICloudOfflinePlayer> getOfflinePlayerAsync(UUID uniqueId) {
        return schedule(() -> getOfflinePlayer(uniqueId));
    }

    @Override
    public ITask<List<? extends ICloudOfflinePlayer>> getOfflinePlayerAsync(String name) {
        return schedule(() -> getOfflinePlayer(name));
    }

    @Override
    public ITask<List<? extends ICloudOfflinePlayer>> getRegisteredPlayersAsync() {
        return schedule(this::getRegisteredPlayers);
    }


    @Override
    public void updateOfflinePlayer(ICloudOfflinePlayer cloudOfflinePlayer) {
        Validate.checkNotNull(cloudOfflinePlayer);

        updateOfflinePlayer0(cloudOfflinePlayer);
        CloudNetDriver.getInstance().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "update_offline_cloud_player",
                new JsonDocument(
                        "offlineCloudPlayer", cloudOfflinePlayer
                )
        );
    }

    public void updateOfflinePlayer0(ICloudOfflinePlayer cloudOfflinePlayer) {
        getDatabase().update(cloudOfflinePlayer.getUniqueId().toString(), JsonDocument.newDocument(cloudOfflinePlayer));
    }

    @Override
    public void updateOnlinePlayer(ICloudPlayer cloudPlayer) {
        Validate.checkNotNull(cloudPlayer);

        updateOnlinePlayer0(cloudPlayer);
        CloudNetDriver.getInstance().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "update_online_cloud_player",
                new JsonDocument(
                        "cloudPlayer", cloudPlayer
                )
        );
    }

    public void updateOnlinePlayer0(ICloudPlayer cloudPlayer) {
        updateOfflinePlayer0(CloudOfflinePlayer.of(cloudPlayer));
    }

    @Override
    public void proxySendPlayer(ICloudPlayer cloudPlayer, String serviceName) {
        Validate.checkNotNull(cloudPlayer);
        Validate.checkNotNull(serviceName);

        CloudNetDriver.getInstance().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "send_on_proxy_player_to_server",
                new JsonDocument()
                        .append("uniqueId", cloudPlayer.getUniqueId())
                        .append("serviceName", serviceName)
        );
    }

    @Override
    public void proxySendPlayerMessage(ICloudPlayer cloudPlayer, String message) {
        Validate.checkNotNull(cloudPlayer);
        Validate.checkNotNull(message);

        CloudNetDriver.getInstance().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "send_message_to_proxy_player",
                new JsonDocument()
                        .append("uniqueId", cloudPlayer.getUniqueId())
                        .append("name", cloudPlayer.getName())
                        .append("message", message)
        );
    }

    @Override
    public void proxyKickPlayer(ICloudPlayer cloudPlayer, String kickMessage) {
        Validate.checkNotNull(cloudPlayer);
        Validate.checkNotNull(kickMessage);

        CloudNetDriver.getInstance().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "kick_on_proxy_player_from_network",
                new JsonDocument()
                        .append("uniqueId", cloudPlayer.getUniqueId())
                        .append("name", cloudPlayer.getName())
                        .append("kickMessage", kickMessage)
        );
    }

    @Override
    public void broadcastMessage(String message) {
        Validate.checkNotNull(message);

        CloudNetDriver.getInstance().sendChannelMessage(
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

        CloudNetDriver.getInstance().sendChannelMessage(
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