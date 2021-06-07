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

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.ext.bridge.player.executor.PlayerExecutor;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IPlayerManager {

  /**
   * Gets the amount of online players on the network
   *
   * @return the online count as an int
   */
  int getOnlineCount();

  /**
   * Gets the amount of registered players in the database.
   *
   * @return the registered player count as an int
   */
  long getRegisteredCount();

  /**
   * Gets an online player by its UUID.
   *
   * @param uniqueId the UUID of the player
   * @return the player if he is online or null if not
   */
  @Nullable
  ICloudPlayer getOnlinePlayer(@NotNull UUID uniqueId);

  /**
   * Gets the first online player found by its name.
   *
   * @param name the name of the player
   * @return the online player if there is at least one player with the given name online or null if there is no player
   * with that name online
   */
  @Nullable
  default ICloudPlayer getFirstOnlinePlayer(@NotNull String name) {
    List<? extends ICloudPlayer> players = this.getOnlinePlayers(name);
    return players.isEmpty() ? null : players.get(0);
  }

  /**
   * Gets a list of all online players with the given name (case-insensitive).
   *
   * @param name the name of the player(s)
   * @return a list containing all online players in the cloud with the given name
   * @deprecated Moved to {@link #getOnlinePlayers(String)}
   */
  @Deprecated
  @ApiStatus.ScheduledForRemoval(inVersion = "3.6")
  default List<? extends ICloudPlayer> getOnlinePlayer(@NotNull String name) {
    return this.getOnlinePlayers(name);
  }

  /**
   * Gets a list of all online players with the given name. (case-insensitive).
   *
   * @param name the name of the player(s)
   * @return a list containing all online players in the cloud with the given name
   */
  @NotNull
  List<? extends ICloudPlayer> getOnlinePlayers(@NotNull String name);

  /**
   * Gets a list of all online players on a specific environment.
   *
   * @param environment the environment to get all players from
   * @return a list containing all players that are online on the given environment
   */
  @NotNull
  List<? extends ICloudPlayer> getOnlinePlayers(@NotNull ServiceEnvironmentType environment);

  /**
   * Gets a list of all online players on the whole network.
   *
   * @return a list containing all players that are online on the network
   * @deprecated Replace with {@link #onlinePlayers()}
   */
  @NotNull
  @Deprecated
  List<? extends ICloudPlayer> getOnlinePlayers();

  /**
   * Gets a PlayerProvider which returns a list of all online players on the whole network.
   */
  @NotNull
  PlayerProvider onlinePlayers();

  /**
   * Gets a PlayerProvider which returns a list of all online players on a specific task.
   *
   * @return a list containing all players that are online on that task
   */
  @NotNull
  PlayerProvider taskOnlinePlayers(@NotNull String task);

  /**
   * Gets a PlayerProvider which returns a list of all online players on a specific group.
   */
  @NotNull
  PlayerProvider groupOnlinePlayers(@NotNull String group);

  /**
   * Gets a registered player by its UUID out of the cloud
   *
   * @param uniqueId the UUID of the player
   * @return the player if he is registered in the cloud or null if not
   */
  @Nullable
  ICloudOfflinePlayer getOfflinePlayer(@NotNull UUID uniqueId);

  /**
   * Gets the first registered player found by its name.
   *
   * @param name the name of the player
   * @return the registered player if there is at least one player with the given name registered or null if there is no
   * player with that name registered
   */
  @Nullable
  default ICloudOfflinePlayer getFirstOfflinePlayer(@NotNull String name) {
    List<? extends ICloudOfflinePlayer> players = this.getOfflinePlayers(name);
    return players.isEmpty() ? null : players.get(0);
  }

  /**
   * Gets a list of all registered players with the given name. (case-sensitive)
   *
   * @param name the name of the player(s)
   * @return a list containing all players registered in the cloud with the given name
   * @deprecated Moved to {@link #getOfflinePlayers(String)}
   */
  @Deprecated
  @ApiStatus.ScheduledForRemoval(inVersion = "3.6")
  default List<? extends ICloudOfflinePlayer> getOfflinePlayer(@NotNull String name) {
    return this.getOfflinePlayers(name);
  }

  /**
   * Gets a list of all registered players with the given name. (case-sensitive)
   *
   * @param name the name of the player(s)
   * @return a list containing all players registered in the cloud with the given name
   */
  @NotNull
  List<? extends ICloudOfflinePlayer> getOfflinePlayers(@NotNull String name);

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
  @ApiStatus.Experimental
  List<? extends ICloudOfflinePlayer> getRegisteredPlayers();

  /**
   * Gets the amount of online players on the network
   *
   * @return the online count as an int
   */
  @NotNull
  ITask<Integer> getOnlineCountAsync();

  /**
   * Gets the amount of registered players in the database.
   *
   * @return the registered player count as an int
   */
  @NotNull
  ITask<Long> getRegisteredCountAsync();

  /**
   * Gets an online player by its UUID.
   *
   * @param uniqueId the UUID of the player
   * @return the player if he is online or null if not
   */
  @NotNull
  ITask<? extends ICloudPlayer> getOnlinePlayerAsync(@NotNull UUID uniqueId);

  /**
   * Gets the first online player found by its name.
   *
   * @param name the name of the player
   * @return the online player if there is at least one player with the given name online or null if there is no player
   * with that name online
   */
  @NotNull
  default ITask<ICloudPlayer> getFirstOnlinePlayerAsync(@NotNull String name) {
    return this.getOnlinePlayersAsync(name).map(players -> players.isEmpty() ? null : players.get(0));
  }

  /**
   * Gets a list of all online players with the given name (case-insensitive).
   *
   * @param name the name of the player(s)
   * @return a list containing all online players in the cloud with the given name
   * @deprecated Moved to {@link #getOnlinePlayersAsync(String)}
   */
  @Deprecated
  @ApiStatus.ScheduledForRemoval(inVersion = "3.6")
  @NotNull
  default ITask<List<? extends ICloudPlayer>> getOnlinePlayerAsync(@NotNull String name) {
    return this.getOnlinePlayersAsync(name);
  }

  /**
   * Gets a list of all online players with the given name. (case-insensitive).
   *
   * @param name the name of the player(s)
   * @return a list containing all online players in the cloud with the given name
   */
  @NotNull
  ITask<List<? extends ICloudPlayer>> getOnlinePlayersAsync(@NotNull String name);

  /**
   * Gets a list of all online players on a specific environment.
   *
   * @param environment the environment to get all players from
   * @return a list containing all players that are online on the given environment
   */
  @NotNull
  ITask<List<? extends ICloudPlayer>> getOnlinePlayersAsync(@NotNull ServiceEnvironmentType environment);

  /**
   * Gets a list of all online players on the whole network.
   *
   * @return a list containing all players that are online on the network
   * @deprecated Replace with {@link #onlinePlayers()}
   */
  @NotNull
  @Deprecated
  ITask<List<? extends ICloudPlayer>> getOnlinePlayersAsync();

  /**
   * Gets a list of all online players on the whole network.
   *
   * @return a list containing all players that are online on the network
   */
  @NotNull
  ITask<ICloudOfflinePlayer> getOfflinePlayerAsync(@NotNull UUID uniqueId);

  /**
   * Gets a list of all registered players with the given name. (case-insensitive)
   *
   * @param name the name of the player(s)
   * @return a list containing all players registered in the cloud with the given name
   * @deprecated Moved to {@link #getOfflinePlayersAsync(String)}
   */
  @Deprecated
  @ApiStatus.ScheduledForRemoval(inVersion = "3.6")
  @NotNull
  default ITask<List<? extends ICloudOfflinePlayer>> getOfflinePlayerAsync(@NotNull String name) {
    return this.getOfflinePlayersAsync(name);
  }

  /**
   * Gets the first registered player found by its name.
   *
   * @param name the name of the player
   * @return the registered player if there is at least one player with the given name registered or null if there is no
   * player with that name registered
   */
  @NotNull
  default ITask<ICloudOfflinePlayer> getFirstOfflinePlayerAsync(@NotNull String name) {
    return this.getOfflinePlayersAsync(name).map(players -> players.isEmpty() ? null : players.get(0));
  }

  /**
   * Gets a list of all online players with the given name. (case-insensitive)
   *
   * @param name the name of the player(s)
   * @return a list containing all players registered in the cloud with the given name
   */
  @NotNull
  ITask<List<? extends ICloudOfflinePlayer>> getOfflinePlayersAsync(@NotNull String name);

  /**
   * Gets a list of all registered players in the network.
   * <p>
   * Depending on the amount of registered players, this method might take a bit longer or won't even work, because it
   * takes too much memory to keep the whole list loaded.
   *
   * @return the list with every registered player in the cloud
   * @deprecated This shouldn't be used when you have many players in your database, because it can cause major problems
   * in the cloud
   */
  @Deprecated
  @ApiStatus.ScheduledForRemoval(inVersion = "3.6")
  @NotNull
  ITask<List<? extends ICloudOfflinePlayer>> getRegisteredPlayersAsync();

  /**
   * Updates the given player to the database of the cloud and calls an update event on the whole network.
   *
   * @param cloudOfflinePlayer the player to be updated
   */
  void updateOfflinePlayer(@NotNull ICloudOfflinePlayer cloudOfflinePlayer);

  /**
   * Updates the given player to the database of the cloud and calls the player update event.
   *
   * @param cloudPlayer the player to be updated
   */
  void updateOnlinePlayer(@NotNull ICloudPlayer cloudPlayer);

  /**
   * Deletes the given player from the database and notifies all connected components.
   *
   * @param cloudOfflinePlayer the player to be deleted
   */
  void deleteCloudOfflinePlayer(@NotNull ICloudOfflinePlayer cloudOfflinePlayer);

  /**
   * Deletes the given player from the database async and notifies all connected components.
   *
   * @param cloudOfflinePlayer the player to be deleted
   */
  ITask<Void> deleteCloudOfflinePlayerAsync(@NotNull ICloudOfflinePlayer cloudOfflinePlayer);

  /**
   * Gets the player executor which will execute the methods as every player that is online on the proxies.
   *
   * @return the constant {@link PlayerExecutor}
   */
  PlayerExecutor getGlobalPlayerExecutor();

  /**
   * Creates a new player executor to interact with the given player.
   *
   * @param cloudPlayer the player to interact with
   * @return a new {@link PlayerExecutor}
   */
  @NotNull
  default PlayerExecutor getPlayerExecutor(@NotNull ICloudPlayer cloudPlayer) {
    return this.getPlayerExecutor(cloudPlayer.getUniqueId());
  }

  /**
   * Creates a new player executor to interact with the given player.
   *
   * @param uniqueId the uniqueId of the player to interact with
   * @return a new {@link PlayerExecutor}
   */
  @NotNull
  PlayerExecutor getPlayerExecutor(@NotNull UUID uniqueId);

  /**
   * Connects an online player to a specific service.
   *
   * @param cloudPlayer the player to be connected
   * @param serviceName the name of the service the player should be sent to
   * @deprecated use {@link #getPlayerExecutor(ICloudPlayer)}
   */
  @Deprecated
  @ApiStatus.ScheduledForRemoval(inVersion = "3.6")
  default void proxySendPlayer(@NotNull ICloudPlayer cloudPlayer, @NotNull String serviceName) {
    this.getPlayerExecutor(cloudPlayer).connect(serviceName);
  }

  /**
   * Connects an online player to a specific service.
   *
   * @param uniqueId    the uuid of the player to be connected
   * @param serviceName the name of the service the player should be sent to
   * @deprecated use {@link #getPlayerExecutor(UUID)}
   */
  @Deprecated
  @ApiStatus.ScheduledForRemoval(inVersion = "3.6")
  default void proxySendPlayer(@NotNull UUID uniqueId, @NotNull String serviceName) {
    this.getPlayerExecutor(uniqueId).connect(serviceName);
  }

  /**
   * Kicks an online player from the network with a specific reason.
   *
   * @param cloudPlayer the player to be kicked
   * @param message     the reason for the kick which will be displayed in the players client
   * @deprecated use {@link #getPlayerExecutor(ICloudPlayer)}
   */
  @Deprecated
  @ApiStatus.ScheduledForRemoval(inVersion = "3.6")
  default void proxyKickPlayer(@NotNull ICloudPlayer cloudPlayer, @NotNull String message) {
    this.getPlayerExecutor(cloudPlayer).kick(message);
  }

  /**
   * Kicks an online player from the network with a specific reason.
   *
   * @param uniqueId the uuid of the player to be kicked
   * @param message  the reason for the kick which will be displayed in the players client
   * @deprecated use {@link #getPlayerExecutor(UUID)}
   */
  @Deprecated
  @ApiStatus.ScheduledForRemoval(inVersion = "3.6")
  default void proxyKickPlayer(@NotNull UUID uniqueId, @NotNull String message) {
    this.getPlayerExecutor(uniqueId).kick(message);
  }

  /**
   * Sends a message to a specific online player.
   *
   * @param cloudPlayer the player to send the message to
   * @param message     the message to be sent to the player
   * @deprecated use {@link #getPlayerExecutor(ICloudPlayer)}
   */
  @Deprecated
  @ApiStatus.ScheduledForRemoval(inVersion = "3.6")
  default void proxySendPlayerMessage(@NotNull ICloudPlayer cloudPlayer, @NotNull String message) {
    this.getPlayerExecutor(cloudPlayer).sendChatMessage(message);
  }

  /**
   * Sends a plugin message to a specific online player.
   *
   * @param uniqueId the uuid of the player to send the message to
   * @param message  the message to be sent to the player
   * @deprecated use {@link #getPlayerExecutor(UUID)}
   */
  @Deprecated
  @ApiStatus.ScheduledForRemoval(inVersion = "3.6")
  default void proxySendPlayerMessage(@NotNull UUID uniqueId, @NotNull String message) {
    this.getPlayerExecutor(uniqueId).sendChatMessage(message);
  }

  /**
   * Sends a plugin message to a specific online player.
   *
   * @param cloudPlayer the player to send the message to
   * @param tag         the tag of the plugin message
   * @param data        the data of the plugin message
   * @deprecated use {@link #getPlayerExecutor(ICloudPlayer)}
   */
  @Deprecated
  @ApiStatus.ScheduledForRemoval(inVersion = "3.6")
  default void proxySendPluginMessage(ICloudPlayer cloudPlayer, String tag, byte[] data) {
    this.getPlayerExecutor(cloudPlayer).sendPluginMessage(tag, data);
  }

  /**
   * Sends a message to a specific online player.
   *
   * @param uniqueId the uuid of the player to send the message to
   * @param tag      the tag of the plugin message
   * @param data     the data of the plugin message
   * @deprecated use {@link #getPlayerExecutor(UUID)}
   */
  @Deprecated
  @ApiStatus.ScheduledForRemoval(inVersion = "3.6")
  default void proxySendPluginMessage(UUID uniqueId, String tag, byte[] data) {
    this.getPlayerExecutor(uniqueId).sendPluginMessage(tag, data);
  }

  /**
   * Broadcasts a specific message over the whole network.
   *
   * @param message the message to be sent to all online players
   * @deprecated replace with {@link #getGlobalPlayerExecutor()}
   */
  @Deprecated
  void broadcastMessage(@NotNull String message);

  /**
   * Broadcasts a specific message to all online players that have the given permission. If the given permission is
   * null, no permission is checked and the message is sent to all online players.
   *
   * @param message    the message to be sent to all online players with the given permission
   * @param permission the permission to check for
   * @deprecated replace with {@link #getGlobalPlayerExecutor()}
   */
  @Deprecated
  void broadcastMessage(@NotNull String message, @Nullable String permission);

}
