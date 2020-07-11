package de.dytanic.cloudnet.ext.bridge.node;

import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.ext.bridge.node.player.NodePlayerManager;
import de.dytanic.cloudnet.ext.bridge.player.ICloudOfflinePlayer;
import de.dytanic.cloudnet.ext.bridge.player.ICloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.PlayerProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NodePlayerProvider implements PlayerProvider {

    private final NodePlayerManager playerManager;
    private final Supplier<Stream<? extends ICloudPlayer>> playerSupplier;

    public NodePlayerProvider(NodePlayerManager playerManager, Supplier<Stream<? extends ICloudPlayer>> playerSupplier) {
        this.playerManager = playerManager;
        this.playerSupplier = playerSupplier;
    }

    @Override
    public @NotNull Collection<? extends ICloudPlayer> asPlayers() {
        return this.playerSupplier.get().collect(Collectors.toList());
    }

    @Override
    public @NotNull Collection<UUID> asUUIDs() {
        return this.playerSupplier.get().map(ICloudOfflinePlayer::getUniqueId).collect(Collectors.toList());
    }

    @Override
    public @NotNull Collection<String> asNames() {
        return this.playerSupplier.get().map(INameable::getName).collect(Collectors.toList());
    }

    @Override
    public int count() {
        return (int) this.playerSupplier.get().count();
    }

    @Override
    public @NotNull ITask<Collection<? extends ICloudPlayer>> asPlayersAsync() {
        return this.playerManager.schedule(this::asPlayers);
    }

    @Override
    public @NotNull ITask<Collection<UUID>> asUUIDsAsync() {
        return this.playerManager.schedule(this::asUUIDs);
    }

    @Override
    public @NotNull ITask<Collection<String>> asNamesAsync() {
        return this.playerManager.schedule(this::asNames);
    }

    @Override
    public @NotNull ITask<Integer> countAsync() {
        return this.playerManager.schedule(this::count);
    }
}
