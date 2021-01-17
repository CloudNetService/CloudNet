package de.dytanic.cloudnet.ext.bridge.node.player;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.concurrent.CompletedTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.database.Database;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceId;
import de.dytanic.cloudnet.ext.bridge.node.NodePlayerProvider;
import de.dytanic.cloudnet.ext.bridge.player.CloudOfflinePlayer;
import de.dytanic.cloudnet.ext.bridge.player.CloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.DefaultPlayerManager;
import de.dytanic.cloudnet.ext.bridge.player.ICloudOfflinePlayer;
import de.dytanic.cloudnet.ext.bridge.player.ICloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkPlayerServerInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkServiceInfo;
import de.dytanic.cloudnet.ext.bridge.player.PlayerProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@ApiStatus.Internal
public final class NodePlayerManager extends DefaultPlayerManager implements IPlayerManager {

    private final Map<UUID, CloudPlayer> onlineCloudPlayers = new ConcurrentHashMap<>();

    private final String databaseName;

    private final PlayerProvider allPlayersProvider = new NodePlayerProvider(this, () -> this.onlineCloudPlayers.values().stream());

    public NodePlayerManager(String databaseName) {
        this.databaseName = databaseName;
    }

    /**
     * @deprecated IPlayerManager should be accessed through the {@link de.dytanic.cloudnet.common.registry.IServicesRegistry}
     */
    @Deprecated
    public static NodePlayerManager getInstance() {
        return (NodePlayerManager) CloudNetDriver.getInstance().getServicesRegistry().getFirstService(IPlayerManager.class);
    }

    public Database getDatabase() {
        return CloudNet.getInstance().getDatabaseProvider().getDatabase(this.databaseName);
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
        return this.onlineCloudPlayers.get(uniqueId);
    }

    @Override
    public @NotNull List<? extends ICloudPlayer> getOnlinePlayers(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return this.onlineCloudPlayers.values().stream().filter(cloudPlayer -> cloudPlayer.getName().equalsIgnoreCase(name)).collect(Collectors.toList());
    }

    @Override
    public @NotNull List<? extends ICloudPlayer> getOnlinePlayers(@NotNull ServiceEnvironmentType environment) {
        Preconditions.checkNotNull(environment);

        return this.onlineCloudPlayers.values().stream().filter(cloudPlayer -> (cloudPlayer.getLoginService() != null && cloudPlayer.getLoginService().getEnvironment() == environment) ||
                (cloudPlayer.getConnectedService() != null && cloudPlayer.getConnectedService().getEnvironment() == environment)).collect(Collectors.toList());
    }

    @Override
    public @NotNull List<CloudPlayer> getOnlinePlayers() {
        return new ArrayList<>(this.onlineCloudPlayers.values());
    }

    @Override
    public @NotNull PlayerProvider onlinePlayers() {
        return this.allPlayersProvider;
    }

    @Override
    public @NotNull PlayerProvider taskOnlinePlayers(@NotNull String task) {
        return new NodePlayerProvider(this,
                () -> this.onlineCloudPlayers.values().stream()
                        .filter(cloudPlayer ->
                                cloudPlayer.getConnectedService().getTaskName().equalsIgnoreCase(task) ||
                                        cloudPlayer.getLoginService().getTaskName().equalsIgnoreCase(task)
                        )
        );
    }

    @Override
    public @NotNull PlayerProvider groupOnlinePlayers(@NotNull String group) {
        return new NodePlayerProvider(this,
                () -> this.onlineCloudPlayers.values().stream()
                        .filter(cloudPlayer ->
                                Arrays.stream(cloudPlayer.getConnectedService().getGroups()).anyMatch(s -> s.equalsIgnoreCase(group)) ||
                                        Arrays.stream(cloudPlayer.getLoginService().getGroups()).anyMatch(s -> s.equalsIgnoreCase(group))
                        )
        );
    }

    @Override
    public ICloudOfflinePlayer getOfflinePlayer(@NotNull UUID uniqueId) {
        Preconditions.checkNotNull(uniqueId);

        JsonDocument jsonDocument = this.getDatabase().get(uniqueId.toString());

        return jsonDocument != null ? this.parseOfflinePlayer(jsonDocument) : null;
    }

    @Override
    public @NotNull List<? extends ICloudOfflinePlayer> getOfflinePlayers(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return this.getDatabase().get(new JsonDocument("name", name)).stream()
                .map(this::parseOfflinePlayer)
                .collect(Collectors.toList());
    }

    @Override
    @Deprecated
    public List<? extends ICloudOfflinePlayer> getRegisteredPlayers() {
        List<CloudOfflinePlayer> cloudOfflinePlayers = new ArrayList<>();

        this.getDatabase().iterate((s, jsonDocument) -> cloudOfflinePlayers.add(this.parseOfflinePlayer(jsonDocument)));

        return cloudOfflinePlayers;
    }

    private CloudOfflinePlayer parseOfflinePlayer(JsonDocument jsonDocument) {
        CloudOfflinePlayer cloudOfflinePlayer = jsonDocument.toInstanceOf(CloudOfflinePlayer.TYPE);

        NetworkServiceInfo networkServiceInfo = cloudOfflinePlayer.getLastNetworkConnectionInfo().getNetworkService();

        if (networkServiceInfo.getServiceId() == null || networkServiceInfo.getGroups() == null) {
            // CloudNet 3.3 and lower CloudOfflinePlayer database entries don't have a serviceId and groups, migrating them
            JsonDocument lastNetworkConnectionInfoDocument = jsonDocument.getDocument("lastNetworkConnectionInfo", new JsonDocument());
            JsonDocument networkServiceDocument = lastNetworkConnectionInfoDocument.getDocument("networkService", new JsonDocument());

            String[] serverNameSplit = networkServiceDocument.getString("serverName", "").split("-");

            ServiceId serviceId = new ServiceId(
                    networkServiceDocument.get("uniqueId", UUID.class, UUID.randomUUID()),
                    CloudNetDriver.getInstance().getComponentName(),
                    serverNameSplit.length > 0 ? serverNameSplit[0] : "",
                    serverNameSplit.length > 1 ? Integer.parseInt(serverNameSplit[1]) : -1,
                    networkServiceDocument.get("environment", ServiceEnvironmentType.class, ServiceEnvironmentType.MINECRAFT_SERVER)
            );

            networkServiceInfo.setServiceId(serviceId);
            networkServiceInfo.setGroups(new String[0]);
            this.updateOfflinePlayer0(cloudOfflinePlayer);
        }

        return cloudOfflinePlayer;
    }

    @Override
    @NotNull
    public ITask<Integer> getOnlineCountAsync() {
        return CompletedTask.create(this.onlineCloudPlayers.size());
    }

    @Override
    @NotNull
    public ITask<Long> getRegisteredCountAsync() {
        return this.getDatabase().getDocumentsCountAsync();
    }

    @Override
    @NotNull
    public ITask<ICloudPlayer> getOnlinePlayerAsync(@NotNull UUID uniqueId) {
        return this.schedule(() -> this.getOnlinePlayer(uniqueId));
    }

    @Override
    @NotNull
    public ITask<List<? extends ICloudPlayer>> getOnlinePlayersAsync(@NotNull String name) {
        return this.schedule(() -> this.getOnlinePlayers(name));
    }

    @Override
    @NotNull
    public ITask<List<? extends ICloudPlayer>> getOnlinePlayersAsync(@NotNull ServiceEnvironmentType environment) {
        return this.schedule(() -> this.getOnlinePlayers(environment));
    }

    @Override
    @NotNull
    public ITask<List<? extends ICloudPlayer>> getOnlinePlayersAsync() {
        return this.schedule(this::getOnlinePlayers);
    }

    @Override
    @NotNull
    public ITask<ICloudOfflinePlayer> getOfflinePlayerAsync(@NotNull UUID uniqueId) {
        return this.schedule(() -> this.getOfflinePlayer(uniqueId));
    }

    @Override
    @NotNull
    public ITask<List<? extends ICloudOfflinePlayer>> getOfflinePlayersAsync(@NotNull String name) {
        return this.schedule(() -> this.getOfflinePlayers(name));
    }

    @Override
    @NotNull
    public ITask<List<? extends ICloudOfflinePlayer>> getRegisteredPlayersAsync() {
        return this.schedule(this::getRegisteredPlayers);
    }

    @Override
    public void updateOfflinePlayer(@NotNull ICloudOfflinePlayer cloudOfflinePlayer) {
        Preconditions.checkNotNull(cloudOfflinePlayer);

        this.updateOfflinePlayer0(cloudOfflinePlayer);
        this.messageBuilder()
                .message("update_offline_cloud_player")
                .buffer(ProtocolBuffer.create().writeObject(cloudOfflinePlayer))
                .targetAll()
                .build()
                .send();
    }

    public void updateOfflinePlayer0(ICloudOfflinePlayer cloudOfflinePlayer) {
        this.getDatabase().update(cloudOfflinePlayer.getUniqueId().toString(), JsonDocument.newDocument(cloudOfflinePlayer));
    }

    @Override
    public void updateOnlinePlayer(@NotNull ICloudPlayer cloudPlayer) {
        Preconditions.checkNotNull(cloudPlayer);

        this.updateOnlinePlayer0(cloudPlayer);
        this.messageBuilder()
                .message("update_online_cloud_player")
                .buffer(ProtocolBuffer.create().writeObject(cloudPlayer))
                .targetAll()
                .build()
                .send();
    }

    public void updateOnlinePlayer0(ICloudPlayer cloudPlayer) {
        if (this.onlineCloudPlayers.containsKey(cloudPlayer.getUniqueId())) {
            this.onlineCloudPlayers.put(cloudPlayer.getUniqueId(), (CloudPlayer) cloudPlayer);
        }
        this.updateOfflinePlayer0(CloudOfflinePlayer.of(cloudPlayer));
    }


    @NotNull
    public <T> ITask<T> schedule(Callable<T> callable) {
        return CloudNet.getInstance().getTaskScheduler().schedule(callable);
    }

    public Map<UUID, CloudPlayer> getOnlineCloudPlayers() {
        return this.onlineCloudPlayers;
    }

    public String getDatabaseName() {
        return this.databaseName;
    }

    public void loginPlayer(NetworkConnectionInfo networkConnectionInfo, NetworkPlayerServerInfo networkPlayerServerInfo) {
        CloudPlayer cloudPlayer = this.getOnlinePlayer(networkConnectionInfo.getUniqueId());
        NetworkServiceInfo networkService = networkConnectionInfo.getNetworkService();

        if (cloudPlayer == null) {
            cloudPlayer = this.getOnlineCloudPlayers().values().stream()
                    .filter(cloudPlayer1 -> cloudPlayer1.getName().equalsIgnoreCase(networkConnectionInfo.getName()) &&
                            cloudPlayer1.getLoginService().getUniqueId().equals(networkService.getUniqueId()))
                    .findFirst()
                    .orElse(null);

            if (cloudPlayer == null) {
                ICloudOfflinePlayer cloudOfflinePlayer = this.getOrRegisterOfflinePlayer(networkConnectionInfo);

                cloudPlayer = new CloudPlayer(
                        cloudOfflinePlayer,
                        networkService,
                        networkService,
                        networkConnectionInfo,
                        networkPlayerServerInfo
                );

                cloudPlayer.setLastLoginTimeMillis(System.currentTimeMillis());
                this.getOnlineCloudPlayers().put(cloudPlayer.getUniqueId(), cloudPlayer);
            }
        }

        ServiceEnvironmentType environmentType = networkService.getServiceId().getEnvironment();

        if (environmentType.isMinecraftServer()) {
            cloudPlayer.setConnectedService(networkService);

            if (networkPlayerServerInfo != null) {
                cloudPlayer.setNetworkPlayerServerInfo(networkPlayerServerInfo);

                if (networkPlayerServerInfo.getXBoxId() != null) {
                    cloudPlayer.setXBoxId(networkPlayerServerInfo.getXBoxId());
                }
            }
        } else {
            cloudPlayer.setLoginService(networkService);
        }

        cloudPlayer.setName(networkConnectionInfo.getName());
        this.updateOnlinePlayer0(cloudPlayer);
    }

    public ICloudOfflinePlayer getOrRegisterOfflinePlayer(NetworkConnectionInfo networkConnectionInfo) {
        ICloudOfflinePlayer cloudOfflinePlayer = this.getOfflinePlayer(networkConnectionInfo.getUniqueId());

        if (cloudOfflinePlayer == null) {
            cloudOfflinePlayer = new CloudOfflinePlayer(
                    networkConnectionInfo.getUniqueId(),
                    networkConnectionInfo.getName(),
                    null,
                    System.currentTimeMillis(),
                    System.currentTimeMillis(),
                    networkConnectionInfo
            );

            this.getDatabase().insert(
                    cloudOfflinePlayer.getUniqueId().toString(),
                    JsonDocument.newDocument(cloudOfflinePlayer)
            );
        }

        return cloudOfflinePlayer;
    }

    public void logoutPlayer(CloudPlayer cloudPlayer) {
        this.getOnlineCloudPlayers().remove(cloudPlayer.getUniqueId());
        cloudPlayer.setLastNetworkConnectionInfo(cloudPlayer.getNetworkConnectionInfo());
        this.updateOnlinePlayer0(cloudPlayer);
    }

    private void logoutPlayer(UUID uniqueId, String name, Predicate<CloudPlayer> predicate) {
        CloudPlayer cloudPlayer = uniqueId != null ?
                this.getOnlinePlayer(uniqueId) :
                this.getOnlineCloudPlayers().values().stream()
                        .filter(cloudPlayer1 -> cloudPlayer1.getName().equals(name))
                        .findFirst()
                        .orElse(null);

        if (cloudPlayer != null && (predicate == null || predicate.test(cloudPlayer))) {
            this.logoutPlayer(cloudPlayer);
        }
    }

    public void logoutPlayer(NetworkConnectionInfo networkConnectionInfo) {
        this.logoutPlayer(networkConnectionInfo.getUniqueId(), networkConnectionInfo.getName(),
                cloudPlayer -> cloudPlayer != null &&
                        cloudPlayer.getLoginService().getUniqueId().equals(networkConnectionInfo.getNetworkService().getUniqueId())
        );
    }

    public void logoutPlayer(UUID uniqueId, String name) {
        this.logoutPlayer(uniqueId, name, null);
    }

}
