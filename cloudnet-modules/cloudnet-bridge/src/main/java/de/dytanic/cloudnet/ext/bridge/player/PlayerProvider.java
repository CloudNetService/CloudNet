package de.dytanic.cloudnet.ext.bridge.player;

import de.dytanic.cloudnet.common.concurrent.ITask;
import java.util.Collection;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public interface PlayerProvider {

  @NotNull
  Collection<? extends ICloudPlayer> asPlayers();

  @NotNull
  Collection<UUID> asUUIDs();

  @NotNull
  Collection<String> asNames();

  int count();

  @NotNull
  ITask<Collection<? extends ICloudPlayer>> asPlayersAsync();

  @NotNull
  ITask<Collection<UUID>> asUUIDsAsync();

  @NotNull
  ITask<Collection<String>> asNamesAsync();

  @NotNull
  ITask<Integer> countAsync();

}
