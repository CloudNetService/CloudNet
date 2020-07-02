package de.dytanic.cloudnet.ext.bridge.player;

import de.dytanic.cloudnet.common.concurrent.ITask;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;

public interface PlayerProvider {

    @NotNull
    Collection<? extends ICloudPlayer> asPlayers();

    @NotNull
    Collection<UUID> asUUIDs();

    @NotNull
    Collection<String> asNames();

    @NotNull
    ITask<Collection<? extends ICloudPlayer>> asPlayersAsync();

    @NotNull
    ITask<Collection<UUID>> asUUIDsAsync();

    @NotNull
    ITask<Collection<String>> asNamesAsync();

}
