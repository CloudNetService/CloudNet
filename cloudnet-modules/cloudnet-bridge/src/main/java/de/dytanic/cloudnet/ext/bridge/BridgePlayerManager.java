package de.dytanic.cloudnet.ext.bridge;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.ext.bridge.player.*;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.TimeUnit;

@ApiStatus.Internal
public final class BridgePlayerManager extends DefaultPlayerManager implements IPlayerManager {

    private final PlayerProvider allPlayersProvider = new BridgePlayerProvider(this, "online_players", null);

    /**
     * @deprecated IPlayerManager should be accessed through the {@link de.dytanic.cloudnet.common.registry.IServicesRegistry}
     */
    @Deprecated
    public static IPlayerManager getInstance() {
        return CloudNetDriver.getInstance().getServicesRegistry().getFirstService(IPlayerManager.class);
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
        return this.getOnlinePlayerAsync(uniqueId).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    public @NotNull List<? extends ICloudPlayer> getOnlinePlayers(@NotNull String name) {
        return this.getOnlinePlayersAsync(name).get(5, TimeUnit.SECONDS, Collections.emptyList());
    }

    @Override
    public @NotNull List<? extends ICloudPlayer> getOnlinePlayers(@NotNull ServiceEnvironmentType environment) {
        return this.getOnlinePlayersAsync(environment).get(5, TimeUnit.SECONDS, Collections.emptyList());
    }

    @Override
    public @NotNull List<? extends ICloudPlayer> getOnlinePlayers() {
        return new ArrayList<>(this.onlinePlayers().asPlayers());
    }

    @Override
    public @NotNull PlayerProvider onlinePlayers() {
        return this.allPlayersProvider;
    }

    @Override
    public @NotNull PlayerProvider taskOnlinePlayers(@NotNull String task) {
        return new BridgePlayerProvider(this, "online_players_task", ProtocolBuffer.create().writeString(task));
    }

    @Override
    public @NotNull PlayerProvider groupOnlinePlayers(@NotNull String group) {
        return new BridgePlayerProvider(this, "online_players_group", ProtocolBuffer.create().writeString(group));
    }

    @Override
    public ICloudOfflinePlayer getOfflinePlayer(@NotNull UUID uniqueId) {
        return this.getOfflinePlayerAsync(uniqueId).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    public @NotNull List<? extends ICloudOfflinePlayer> getOfflinePlayers(@NotNull String name) {
        return this.getOfflinePlayersAsync(name).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    public List<? extends ICloudOfflinePlayer> getRegisteredPlayers() {
        return this.getRegisteredPlayersAsync().get(5, TimeUnit.MINUTES, null);
    }

    @Override
    @NotNull
    public ITask<Integer> getOnlineCountAsync() {
        return this.messageBuilder()
                .message("get_online_count")
                .targetNode(Wrapper.getInstance().getServiceId().getNodeUniqueId())
                .build()
                .sendSingleQueryAsync()
                .map(message -> message.getBuffer().readInt());
    }

    @Override
    @NotNull
    public ITask<Long> getRegisteredCountAsync() {
        return this.messageBuilder()
                .message("get_registered_count")
                .targetNode(Wrapper.getInstance().getServiceId().getNodeUniqueId())
                .build()
                .sendSingleQueryAsync()
                .map(message -> message.getBuffer().readLong());
    }


    @Override
    @NotNull
    public ITask<? extends ICloudPlayer> getOnlinePlayerAsync(@NotNull UUID uniqueId) {
        Preconditions.checkNotNull(uniqueId);

        return this.messageBuilder()
                .message("get_online_player_by_uuid")
                .buffer(ProtocolBuffer.create().writeUUID(uniqueId))
                .targetNode(Wrapper.getInstance().getServiceId().getNodeUniqueId())
                .build()
                .sendSingleQueryAsync()
                .map(message -> message.getBuffer().readOptionalObject(CloudPlayer.class));
    }

    @Override
    @NotNull
    public ITask<List<? extends ICloudPlayer>> getOnlinePlayersAsync(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return this.messageBuilder()
                .message("get_online_players_by_name")
                .targetNode(Wrapper.getInstance().getServiceId().getNodeUniqueId())
                .buffer(ProtocolBuffer.create().writeString(name))
                .build()
                .sendSingleQueryAsync()
                .map(message -> Arrays.asList(message.getBuffer().readObjectArray(CloudPlayer.class)));
    }

    @Override
    @NotNull
    public ITask<List<? extends ICloudPlayer>> getOnlinePlayersAsync(@NotNull ServiceEnvironmentType environment) {
        Preconditions.checkNotNull(environment);

        return this.messageBuilder()
                .message("get_online_players_by_environment")
                .targetNode(Wrapper.getInstance().getServiceId().getNodeUniqueId())
                .buffer(ProtocolBuffer.create().writeEnumConstant(environment))
                .build()
                .sendSingleQueryAsync()
                .map(message -> Arrays.asList(message.getBuffer().readObjectArray(CloudPlayer.class)));
    }

    @Override
    @NotNull
    public ITask<List<? extends ICloudPlayer>> getOnlinePlayersAsync() {
        return this.onlinePlayers().asPlayersAsync().map(ArrayList::new);
    }

    @Override
    @NotNull
    public ITask<ICloudOfflinePlayer> getOfflinePlayerAsync(@NotNull UUID uniqueId) {
        Preconditions.checkNotNull(uniqueId);

        return this.messageBuilder()
                .message("get_offline_player_by_uuid")
                .targetNode(Wrapper.getInstance().getServiceId().getNodeUniqueId())
                .buffer(ProtocolBuffer.create().writeUUID(uniqueId))
                .build()
                .sendSingleQueryAsync()
                .map(message -> message.getBuffer().readOptionalObject(CloudOfflinePlayer.class));
    }

    @Override
    @NotNull
    public ITask<List<? extends ICloudOfflinePlayer>> getOfflinePlayersAsync(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return this.messageBuilder()
                .message("get_offline_players_by_name")
                .targetNode(Wrapper.getInstance().getServiceId().getNodeUniqueId())
                .buffer(ProtocolBuffer.create().writeString(name))
                .build()
                .sendSingleQueryAsync()
                .map(message -> Arrays.asList(message.getBuffer().readObjectArray(CloudOfflinePlayer.class)));
    }

    @Override
    @NotNull
    public ITask<List<? extends ICloudOfflinePlayer>> getRegisteredPlayersAsync() {
        return this.messageBuilder()
                .message("get_offline_players")
                .targetNode(Wrapper.getInstance().getServiceId().getNodeUniqueId())
                .build()
                .sendSingleQueryAsync()
                .map(message -> Arrays.asList(message.getBuffer().readObjectArray(CloudOfflinePlayer.class)));
    }


    @Override
    public void updateOfflinePlayer(@NotNull ICloudOfflinePlayer cloudOfflinePlayer) {
        Preconditions.checkNotNull(cloudOfflinePlayer);

        this.messageBuilder()
                .message("update_offline_cloud_player")
                .buffer(ProtocolBuffer.create().writeObject(cloudOfflinePlayer))
                .targetAll()
                .build()
                .send();
    }

    @Override
    public void updateOnlinePlayer(@NotNull ICloudPlayer cloudPlayer) {
        Preconditions.checkNotNull(cloudPlayer);

        this.messageBuilder()
                .message("update_online_cloud_player")
                .buffer(ProtocolBuffer.create().writeObject(cloudPlayer))
                .targetAll()
                .build()
                .send();
    }

}