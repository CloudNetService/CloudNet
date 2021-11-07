/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dytanic.cloudnet.ext.bridge.player;

import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.network.rpc.annotation.RPCValidation;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.ext.bridge.player.executor.PlayerExecutor;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

/**
 * This interfaces provides access to cloud players
 */
@RPCValidation
public interface IPlayerManager {

  /**
   * Gets the amount of online players on the network
   *
   * @return the online count as an int
   */
  @Range(from = 0, to = Integer.MAX_VALUE)
  int getOnlineCount();

  /**
   * Gets the amount of registered players in the database.
   *
   * @return the registered player count as an int
   */
  @Range(from = 0, to = Long.MAX_VALUE)
  long getRegisteredCount();

  /**
   * Gets an online player by its UUID.
   *
   * @param uniqueId the UUID of the player
   * @return the player if he is online or null if not
   */
  @Nullable CloudPlayer getOnlinePlayer(@NotNull UUID uniqueId);

  /**
   * Gets the first online player found by its name.
   *
   * @param name the name of the player
   * @return the online player if there is at least one player with the given name online or null if there is no player
   * with that name online
   */
  default @Nullable CloudPlayer getFirstOnlinePlayer(@NotNull String name) {
    List<? extends CloudPlayer> players = this.getOnlinePlayers(name);
    return players.isEmpty() ? null : players.get(0);
  }

  /**
   * Gets a list of all online players with the given name. (case-insensitive).
   *
   * @param name the name of the player(s)
   * @return a list containing all online players in the cloud with the given name
   */
  @NotNull List<? extends CloudPlayer> getOnlinePlayers(@NotNull String name);

  /**
   * Gets a list of all online players on a specific environment.
   *
   * @param environment the environment to get all players from
   * @return a list containing all players that are online on the given environment
   */
  @NotNull List<? extends CloudPlayer> getEnvironmentOnlinePlayers(@NotNull ServiceEnvironmentType environment);

  /**
   * Gets a PlayerProvider which returns a list of all online players on the whole network.
   */
  @NotNull PlayerProvider onlinePlayers();

  /**
   * Gets a PlayerProvider which returns a list of all online players on a specific task.
   *
   * @return a list containing all players that are online on that task
   */
  @NotNull PlayerProvider taskOnlinePlayers(@NotNull String task);

  /**
   * Gets a PlayerProvider which returns a list of all online players on a specific group.
   */
  @NotNull PlayerProvider groupOnlinePlayers(@NotNull String group);

  /**
   * Gets a registered player by its UUID out of the cloud
   *
   * @param uniqueId the UUID of the player
   * @return the player if he is registered in the cloud or null if not
   */
  @Nullable CloudOfflinePlayer getOfflinePlayer(@NotNull UUID uniqueId);

  /**
   * Gets the first registered player found by its name.
   *
   * @param name the name of the player
   * @return the registered player if there is at least one player with the given name registered or null if there is no
   * player with that name registered
   */
  @Nullable
  default CloudOfflinePlayer getFirstOfflinePlayer(@NotNull String name) {
    List<? extends CloudOfflinePlayer> players = this.getOfflinePlayers(name);
    return players.isEmpty() ? null : players.get(0);
  }

  /**
   * Gets a list of all registered players with the given name. (case-sensitive)
   *
   * @param name the name of the player(s)
   * @return a list containing all players registered in the cloud with the given name
   */
  @NotNull
  List<? extends CloudOfflinePlayer> getOfflinePlayers(@NotNull String name);

  /**
   * Gets a list of all registered players in the network.
   * <p>
   * Depending on the amount of registered players, this method might take a bit longer or won't even work, because it
   * takes too much memory to keep the whole list loaded.
   * <p>
   * NOTE: This shouldn't be used when you have many players in your database, because it can cause major problems in
   * the cloud
   *
   * @return the list with every registered player in the cloud
   */
  @Experimental
  @NotNull List<? extends CloudOfflinePlayer> getRegisteredPlayers();

  /**
   * Updates the given player to the database of the cloud and calls an update event on the whole network.
   *
   * @param cloudOfflinePlayer the player to be updated
   */
  void updateOfflinePlayer(@NotNull CloudOfflinePlayer cloudOfflinePlayer);

  /**
   * Updates the given player to the database of the cloud and calls the player update event.
   *
   * @param cloudPlayer the player to be updated
   */
  void updateOnlinePlayer(@NotNull CloudPlayer cloudPlayer);

  /**
   * Deletes the given player from the database and notifies all connected components.
   *
   * @param cloudOfflinePlayer the player to be deleted
   */
  void deleteCloudOfflinePlayer(@NotNull CloudOfflinePlayer cloudOfflinePlayer);

  /**
   * Gets the amount of online players on the network
   *
   * @return the online count as an int
   */
  default @NotNull ITask<Integer> getOnlineCountAsync() {
    return CompletableTask.supply(this::getOnlineCount);
  }

  /**
   * Gets the amount of registered players in the database.
   *
   * @return the registered player count as an int
   */
  default @NotNull ITask<Long> getRegisteredCountAsync() {
    return CompletableTask.supply(this::getRegisteredCount);
  }

  /**
   * Gets an online player by its UUID.
   *
   * @param uniqueId the UUID of the player
   * @return the player if he is online or null if not
   */
  default @NotNull ITask<? extends CloudPlayer> getOnlinePlayerAsync(@NotNull UUID uniqueId) {
    return CompletableTask.supply(() -> this.getOnlinePlayer(uniqueId));
  }

  /**
   * Gets the first online player found by its name.
   *
   * @param name the name of the player
   * @return the online player if there is at least one player with the given name online or null if there is no player
   * with that name online
   */
  @NotNull
  default ITask<CloudPlayer> getFirstOnlinePlayerAsync(@NotNull String name) {
    return CompletableTask.supply(() -> this.getFirstOnlinePlayer(name));
  }

  /**
   * Gets a list of all online players with the given name. (case-insensitive).
   *
   * @param name the name of the player(s)
   * @return a list containing all online players in the cloud with the given name
   */
  default @NotNull ITask<List<? extends CloudPlayer>> getOnlinePlayersAsync(@NotNull String name) {
    return CompletableTask.supply(() -> this.getOnlinePlayers(name));
  }

  /**
   * Gets a list of all online players on a specific environment.
   *
   * @param env the environment to get all players from
   * @return a list containing all players that are online on the given environment
   */
  default @NotNull ITask<List<? extends CloudPlayer>> getOnlinePlayersAsync(@NotNull ServiceEnvironmentType env) {
    return CompletableTask.supply(() -> this.getEnvironmentOnlinePlayers(env));
  }

  /**
   * Gets a list of all online players on the whole network.
   *
   * @return a list containing all players that are online on the network
   */
  default @NotNull ITask<CloudOfflinePlayer> getOfflinePlayerAsync(@NotNull UUID uniqueId) {
    return CompletableTask.supply(() -> this.getOfflinePlayer(uniqueId));
  }

  /**
   * Gets the first registered player found by its name.
   *
   * @param name the name of the player
   * @return the registered player if there is at least one player with the given name registered or null if there is no
   * player with that name registered
   */
  default @NotNull ITask<CloudOfflinePlayer> getFirstOfflinePlayerAsync(@NotNull String name) {
    return CompletableTask.supply(() -> this.getFirstOnlinePlayer(name));
  }

  /**
   * Gets a list of all online players with the given name. (case-insensitive)
   *
   * @param name the name of the player(s)
   * @return a list containing all players registered in the cloud with the given name
   */
  default @NotNull ITask<List<? extends CloudOfflinePlayer>> getOfflinePlayersAsync(@NotNull String name) {
    return CompletableTask.supply(() -> this.getOfflinePlayers(name));
  }

  /**
   * Deletes the given player from the database async and notifies all connected components.
   *
   * @param cloudOfflinePlayer the player to be deleted
   */
  default @NotNull ITask<Void> deleteCloudOfflinePlayerAsync(@NotNull CloudOfflinePlayer cloudOfflinePlayer) {
    return CompletableTask.supply(() -> this.deleteCloudOfflinePlayer(cloudOfflinePlayer));
  }

  /**
   * Updates the given player to the database of the cloud and calls an update event on the whole network.
   *
   * @param cloudOfflinePlayer the player to be updated
   */
  default @NotNull ITask<Void> updateOfflinePlayerAsync(@NotNull CloudOfflinePlayer cloudOfflinePlayer) {
    return CompletableTask.supply(() -> this.updateOfflinePlayer(cloudOfflinePlayer));
  }

  /**
   * Updates the given player to the database of the cloud and calls the player update event.
   *
   * @param cloudPlayer the player to be updated
   */
  default @NotNull ITask<Void> updateOnlinePlayerAsync(@NotNull CloudPlayer cloudPlayer) {
    return CompletableTask.supply(() -> this.updateOnlinePlayer(cloudPlayer));
  }

  /**
   * Gets the player executor which will execute the methods as every player that is online on the proxies.
   *
   * @return the constant {@link PlayerExecutor}
   */
  @NotNull PlayerExecutor getGlobalPlayerExecutor();

  /**
   * Creates a new player executor to interact with the given player.
   *
   * @param uniqueId the uniqueId of the player to interact with
   * @return a new {@link PlayerExecutor}
   */
  @NotNull PlayerExecutor getPlayerExecutor(@NotNull UUID uniqueId);
}
