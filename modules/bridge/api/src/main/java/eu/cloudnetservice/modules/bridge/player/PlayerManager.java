/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.bridge.player;

import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.modules.bridge.player.executor.PlayerExecutor;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

/**
 * The player manager is the main api point to access cloud offline and online players. Accessing the player manager is
 * possible using either {@code ServiceRegistry.first(PlayerManager.class)} or
 * {@code bridgeManagement.playerManager()}.
 *
 * @since 4.0
 */
public interface PlayerManager {

  /**
   * Gets the amount of online players connected to the cloudnet network.
   *
   * @return the online player count.
   */
  @Range(from = 0, to = Integer.MAX_VALUE)
  int onlineCount();

  /**
   * Gets the amount of registered players in the database.
   *
   * @return the registered player count.
   */
  @Range(from = 0, to = Long.MAX_VALUE)
  long registeredCount();

  /**
   * Gets an online cloud player by its unique id from the cache on the node.
   * <p>
   * The online player is cached on the node until the player disconnects.
   *
   * @param uniqueId the unique id of the player.
   * @return the online cloud player or null if the player is not online.
   * @throws NullPointerException if the given unique id is null.
   */
  @Nullable
  CloudPlayer onlinePlayer(@NonNull UUID uniqueId);

  /**
   * Gets the first cloud player that is online and has the given case-insensitive name.
   * <p>
   * All online players are cached on the node while they are online.
   *
   * @param name the name of the cloud player.
   * @return the first cloud player with the given name, null if no player was found.
   * @throws NullPointerException if the given name is null.
   */
  @Nullable
  CloudPlayer firstOnlinePlayer(@NonNull String name);

  /**
   * Gets all online cloud players that have the given case-insensitive name.
   * <p>
   * All online players are cached on the node while they are online.
   *
   * @param name the name of the cloud players.
   * @return a list of all online players with the given name.
   * @throws NullPointerException if the given name is null.
   */
  @NonNull
  List<CloudPlayer> onlinePlayers(@NonNull String name);

  /**
   * Gets all online cloud players that are connected to a service of given service environment.
   * <p>
   * All online players are cached on the node while they are online.
   *
   * @param environment the environment to get all players for.
   * @return a list of all cloud players connected to a service of the given service environment.
   * @throws NullPointerException if the given environment is null.
   */
  @NonNull
  List<CloudPlayer> environmentOnlinePlayers(@NonNull ServiceEnvironmentType environment);

  /**
   * Gets the jvm static player executor for all players that are connected to the network. All methods account for all
   * connected players.
   *
   * @return the global player executor for all players.
   */
  @NonNull
  PlayerExecutor globalPlayerExecutor();

  /**
   * Creates a new player executor for the player associated with the given unique id. The player executor is created
   * even if there is no online player with the given unique id.
   *
   * @param uniqueId the unique id of the player for the new player executor.
   * @return the player executor for the given player.
   * @throws NullPointerException if the given unique id is null.
   */
  @NonNull
  PlayerExecutor playerExecutor(@NonNull UUID uniqueId);

  /**
   * Gets a player provider that provides access to all players that are connected to the cloudnet network.
   *
   * @return the player provider for all online players.
   */
  @NonNull
  PlayerProvider onlinePlayers();

  /**
   * Gets a player provider that provides access to all players that are connected to a service of the task with the
   * given name.
   *
   * @param task the task of the service the player has to be connected to.
   * @return the player provider for the given task.
   * @throws NullPointerException if the given task is null.
   */
  @NonNull
  PlayerProvider taskOnlinePlayers(@NonNull String task);

  /**
   * Gets a player provider that provides access to all players that are connected to a service with the group with the
   * given name.
   *
   * @param group the group of the service the player has to be connected to.
   * @return the player provider for the given group.
   * @throws NullPointerException if the given group is null.
   */
  @NonNull
  PlayerProvider groupOnlinePlayers(@NonNull String group);

  /**
   * Gets the offline player associated with the given unique id. The player must have been previously connected.
   * <p>
   * The cloud offline player is cached on the node and retrieved from the cache if present otherwise loaded from the
   * database.
   *
   * @param uniqueId the unique id of the offline player.
   * @return the offline player with the given unique id or null if there is no player.
   * @throws NullPointerException if the given unique id is null.
   */
  @Nullable
  CloudOfflinePlayer offlinePlayer(@NonNull UUID uniqueId);

  /**
   * Gets the first registered offline player that has the given name. The player must have been previously connected.
   * If the player is cached on the corresponding node which is handling the method call, then the given name check will
   * be done case-insensitive against the underlying cache, if the player data must be queried from the database then
   * this check is case-sensitive.
   *
   * @param name the name of the registered player.
   * @return the first offline player with the given name or null if there is no player with the name.
   * @throws NullPointerException if the given name is null.
   */
  @Nullable
  CloudOfflinePlayer firstOfflinePlayer(@NonNull String name);

  /**
   * Gets all registered cloud players that have the given case-sensitive name. The player must have been previously
   * connected.
   *
   * @param name the name of the registered cloud players.
   * @return a list of all registered players with the given name.
   * @throws NullPointerException if the given name is null.
   */
  @NonNull
  List<CloudOfflinePlayer> offlinePlayers(@NonNull String name);

  /**
   * Gets a list with all registered players from the database.
   * <p>
   * This method should not be used if the database contains a lot of registered players. It can happen that the entire
   * heap of the cloud is used up by this method and therefore errors occur.
   *
   * @return a list of all registered players.
   */
  @NonNull
  List<CloudOfflinePlayer> registeredPlayers();

  /**
   * Updates the given cloud offline player in the database, in the local cache of the node and calls the update in the
   * cluster.
   *
   * @param cloudOfflinePlayer the offline player to update.
   * @throws NullPointerException if the given offline player is null.
   */
  void updateOfflinePlayer(@NonNull CloudOfflinePlayer cloudOfflinePlayer);

  /**
   * Updates the given cloud online player in the cache and calls the update in the cluster.
   *
   * @param cloudPlayer the cloud player to update.
   * @throws NullPointerException if the given player is null.
   */
  void updateOnlinePlayer(@NonNull CloudPlayer cloudPlayer);

  /**
   * Deletes the given offline player from the database, the local cache of the node and calls the deletion in the
   * cluster.
   *
   * @param cloudOfflinePlayer the offline player to delete.
   * @throws NullPointerException if the given offline player is null.
   */
  void deleteCloudOfflinePlayer(@NonNull CloudOfflinePlayer cloudOfflinePlayer);

  /**
   * Gets the amount of online players connected to the cloudnet network asynchronously.
   *
   * @return a task containing the online player count.
   */
  @NonNull
  CompletableFuture<Integer> onlineCountAsync();

  /**
   * Gets the amount of registered players in the database asynchronously.
   *
   * @return a task containing the registered player count.
   */
  @NonNull
  CompletableFuture<Long> registeredCountAsync();

  /**
   * Gets an online cloud player by its unique id asynchronously.
   * <p>
   * The online player is cached on the node until the player disconnects.
   *
   * @param uniqueId the unique id of the player.
   * @return a task containing the online cloud player or an empty task if the player is not online.
   * @throws NullPointerException if the given unique id is null.
   */
  @NonNull
  CompletableFuture<CloudPlayer> onlinePlayersAsync(@NonNull UUID uniqueId);

  /**
   * Gets the first cloud player that is online and has the given case-insensitive name asynchronously.
   * <p>
   * All online players are cached on the node while they are online.
   *
   * @param name the name of the cloud player.
   * @return a task containing the first cloud player with the given name.
   * @throws NullPointerException if the given name is null.
   */
  @NonNull
  CompletableFuture<CloudPlayer> firstOnlinePlayerAsync(@NonNull String name);

  /**
   * Gets all online cloud players that have the given case-insensitive name asynchronously.
   * <p>
   * All online players are cached on the node while they are online.
   *
   * @param name the name of the cloud players.
   * @return a task containing a list of all online players with the given name.
   * @throws NullPointerException if the given name is null.
   */
  @NonNull
  CompletableFuture<List<CloudPlayer>> onlinePlayersAsync(@NonNull String name);

  /**
   * Gets all online cloud players that are connected to a service of given service environment asynchronously.
   * <p>
   * All online players are cached on the node while they are online.
   *
   * @param env the environment to get all players for.
   * @return a task containing a list of all cloud players connected to a service of the given service environment.
   * @throws NullPointerException if the given environment is null.
   */
  @NonNull
  CompletableFuture<List<CloudPlayer>> environmentOnlinePlayersAsync(@NonNull ServiceEnvironmentType env);

  /**
   * Gets the offline player associated with the given unique id asynchronously. The player must have been previously
   * connected.
   * <p>
   * The cloud offline player is cached on the node and retrieved from the cache if present otherwise loaded from the
   * database.
   *
   * @param uniqueId the unique id of the offline player.
   * @return a task containing the offline player with the given unique id.
   * @throws NullPointerException if the given unique id is null.
   */
  @NonNull
  CompletableFuture<CloudOfflinePlayer> offlinePlayerAsync(@NonNull UUID uniqueId);

  /**
   * Gets the first registered offline player that has the given name asynchronously. The player must have been
   * previously connected. If the player is cached on the corresponding node which is handling the method call, then the
   * given name check will be done case-insensitive against the underlying cache, if the player data must be queried
   * from the database then this check is case-sensitive.
   *
   * @param name the name of the registered player.
   * @return a task containing the first offline player with the given name.
   * @throws NullPointerException if the given name is null.
   */
  @NonNull
  CompletableFuture<CloudOfflinePlayer> firstOfflinePlayerAsync(@NonNull String name);

  /**
   * Gets all registered cloud players that have the given case-sensitive name asynchronously. The player must have been
   * previously connected.
   *
   * @param name the name of the registered cloud players.
   * @return a task containing a list of all registered players with the given name.
   * @throws NullPointerException if the given name is null.
   */
  @NonNull
  CompletableFuture<List<CloudOfflinePlayer>> offlinePlayersAsync(@NonNull String name);

  /**
   * Updates the given cloud offline player in the database, in the local cache of the node and calls the update in the
   * cluster asynchronously.
   *
   * @param cloudOfflinePlayer the offline player to update.
   * @return a task completing after the player was updated.
   * @throws NullPointerException if the given offline player is null.
   */
  @NonNull
  CompletableFuture<Void> updateOfflinePlayerAsync(@NonNull CloudOfflinePlayer cloudOfflinePlayer);

  /**
   * Updates the given cloud online player in the cache and calls the update in the cluster asynchronously.
   *
   * @param cloudPlayer the cloud player to update.
   * @return a task completing after the player was updated.
   * @throws NullPointerException if the given player is null.
   */
  @NonNull
  CompletableFuture<Void> updateOnlinePlayerAsync(@NonNull CloudPlayer cloudPlayer);

  /**
   * Deletes the given offline player from the database, the local cache of the node and calls the deletion in the
   * cluster asynchronously.
   *
   * @param cloudOfflinePlayer the offline player to delete.
   * @return a task completing after the player was deleted.
   * @throws NullPointerException if the given offline player is null.
   */
  @NonNull
  CompletableFuture<Void> deleteCloudOfflinePlayerAsync(@NonNull CloudOfflinePlayer cloudOfflinePlayer);
}
