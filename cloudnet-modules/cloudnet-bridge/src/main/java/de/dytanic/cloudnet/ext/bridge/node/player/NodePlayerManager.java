package de.dytanic.cloudnet.ext.bridge.node.player;

import com.google.common.base.Preconditions;
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

import java.util.Base64;
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
    public int getOnlineCount() {
        return this.onlineCloudPlayers.size();
    }

    @Override
    public long getRegisteredCount() {
        return this.getDatabase().getDocumentsCount();
    }

    @Override
    public CloudPlayer getOnlinePlayer(UUID uniqueId) {
        return onlineCloudPlayers.get(uniqueId);
    }

    @Override
    public List<? extends ICloudPlayer> getOnlinePlayers(String name) {
        Validate.checkNotNull(name);

        return Iterables.filter(this.onlineCloudPlayers.values(), cloudPlayer -> cloudPlayer.getName().equalsIgnoreCase(name));
    }

    @Override
    public List<? extends ICloudPlayer> getOnlinePlayers(ServiceEnvironmentType environment) {
        Validate.checkNotNull(environment);

        return Iterables.filter(this.onlineCloudPlayers.values(), cloudPlayer -> (cloudPlayer.getLoginService() != null && cloudPlayer.getLoginService().getEnvironment() == environment) ||
                (cloudPlayer.getConnectedService() != null && cloudPlayer.getConnectedService().getEnvironment() == environment));
    }

    @Override
    public List<? extends ICloudPlayer> getOnlinePlayers() {
        return Iterables.newArrayList(this.onlineCloudPlayers.values());
    }

    @Override
    public ICloudOfflinePlayer getOfflinePlayer(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        JsonDocument jsonDocument = this.getDatabase().get(uniqueId.toString());

        return jsonDocument != null ? jsonDocument.toInstanceOf(CloudOfflinePlayer.TYPE) : null;
    }

    @Override
    public List<? extends ICloudOfflinePlayer> getOfflinePlayers(String name) {
        Validate.checkNotNull(name);

        return Iterables.map(this.getDatabase().get(new JsonDocument("name", name)), jsonDocument -> jsonDocument.toInstanceOf(CloudOfflinePlayer.TYPE));
    }

    @Override
    @Deprecated
    public List<? extends ICloudOfflinePlayer> getRegisteredPlayers() {
        List<? extends ICloudOfflinePlayer> cloudOfflinePlayers = Iterables.newArrayList();

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
    public ITask<ICloudPlayer> getOnlinePlayerAsync(UUID uniqueId) {
        return this.schedule(() -> this.getOnlinePlayer(uniqueId));
    }

    @Override
    public ITask<List<? extends ICloudPlayer>> getOnlinePlayersAsync(String name) {
        return this.schedule(() -> this.getOnlinePlayers(name));
    }

    @Override
    public ITask<List<? extends ICloudPlayer>> getOnlinePlayersAsync(ServiceEnvironmentType environment) {
        return this.schedule(() -> this.getOnlinePlayers(environment));
    }

    @Override
    public ITask<List<? extends ICloudPlayer>> getOnlinePlayersAsync() {
        return this.schedule(this::getOnlinePlayers);
    }

    @Override
    public ITask<ICloudOfflinePlayer> getOfflinePlayerAsync(UUID uniqueId) {
        return this.schedule(() -> this.getOfflinePlayer(uniqueId));
    }

    @Override
    public ITask<List<? extends ICloudOfflinePlayer>> getOfflinePlayersAsync(String name) {
        return this.schedule(() -> this.getOfflinePlayers(name));
    }

    @Override
    public ITask<List<? extends ICloudOfflinePlayer>> getRegisteredPlayersAsync() {
        return this.schedule(this::getRegisteredPlayers);
    }

    @Override
    public void updateOfflinePlayer(ICloudOfflinePlayer cloudOfflinePlayer) {
        Validate.checkNotNull(cloudOfflinePlayer);

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
    public void updateOnlinePlayer(ICloudPlayer cloudPlayer) {
        Validate.checkNotNull(cloudPlayer);

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
    public void proxySendPlayer(ICloudPlayer cloudPlayer, String serviceName) {
        Validate.checkNotNull(cloudPlayer);

        this.proxySendPlayer(cloudPlayer.getUniqueId(), serviceName);
    }

    @Override
    public void proxySendPlayer(UUID uniqueId, String serviceName) {
        Validate.checkNotNull(uniqueId);
        Validate.checkNotNull(serviceName);

        CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "send_on_proxy_player_to_server",
                new JsonDocument()
                        .append("uniqueId", uniqueId)
                        .append("serviceName", serviceName)
        );
    }

    @Override
    public void proxySendPlayerMessage(ICloudPlayer cloudPlayer, String message) {
        Validate.checkNotNull(cloudPlayer);

        this.proxySendPlayerMessage(cloudPlayer.getUniqueId(), message);
    }

    @Override
    public void proxySendPlayerMessage(UUID uniqueId, String message) {
        Validate.checkNotNull(uniqueId);
        Validate.checkNotNull(message);

        CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "send_message_to_proxy_player",
                new JsonDocument()
                        .append("uniqueId", uniqueId)
                        .append("message", message)
        );
    }

    @Override
    public void proxySendPluginMessage(ICloudPlayer cloudPlayer, String tag, byte[] data) {
        Preconditions.checkNotNull(cloudPlayer);

        this.proxySendPluginMessage(cloudPlayer.getUniqueId(), tag, data);
    }

    @Override
    public void proxySendPluginMessage(UUID uniqueId, String tag, byte[] data) {
        Preconditions.checkNotNull(uniqueId);
        Preconditions.checkNotNull(tag);
        Preconditions.checkNotNull(data);

        CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "send_plugin_message_to_proxy_player",
                new JsonDocument()
                        .append("uniqueId", uniqueId)
                        .append("tag", tag)
                        .append("data", Base64.getEncoder().encodeToString(data))
        );
    }

    @Override
    public void proxyKickPlayer(ICloudPlayer cloudPlayer, String kickMessage) {
        Validate.checkNotNull(cloudPlayer);

        this.proxyKickPlayer(cloudPlayer.getUniqueId(), kickMessage);
    }

    @Override
    public void proxyKickPlayer(UUID uniqueId, String kickMessage) {
        Validate.checkNotNull(uniqueId);
        Validate.checkNotNull(kickMessage);

        CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME,
                "kick_on_proxy_player_from_network",
                new JsonDocument()
                        .append("uniqueId", uniqueId)
                        .append("kickMessage", kickMessage)
        );
    }

    @Override
    public void broadcastMessage(String message) {
        Validate.checkNotNull(message);

        CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
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
