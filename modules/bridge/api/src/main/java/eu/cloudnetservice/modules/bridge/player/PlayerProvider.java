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

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;

/**
 * The player provider offers the possibility to get already filtered cloud players. In addition, not always the whole
 * {@link CloudPlayer} object must be sent over the network, but only the really desired result, such as all unique
 * ids.
 * <p>
 * Currently, there are these player providers:
 * <ul>
 *   <li>a player provider for all players {@link PlayerManager#onlinePlayers()}</li>
 *   <li>a player provider for all players on a certain task {@link PlayerManager#taskOnlinePlayers(String)}</li>
 *   <li>a player provider for all players on a certain group {@link PlayerManager#groupOnlinePlayers(String)}</li>
 * </ul>
 *
 * @since 4.0
 */
public interface PlayerProvider {

  /**
   * Gets all players supplied by this player provider as cloud players.
   *
   * @return all supplied cloud players.
   */
  @NonNull
  Collection<CloudPlayer> players();

  /**
   * Gets all unique ids of the players supplied by this player provider.
   *
   * @return all supplied unique ids.
   */
  @NonNull
  Collection<UUID> uniqueIds();

  /**
   * Gets all player names for all players supplied by this player provider.
   *
   * @return all supplied player names.
   */
  @NonNull
  Collection<String> names();

  /**
   * Gets the count of supplied players by this player provider.
   *
   * @return the amount of supplied players.
   */
  int count();

  /**
   * Gets all players supplied by this player provider as cloud players asynchronously.
   *
   * @return a task containing all supplied cloud players.
   */
  @NonNull
  CompletableFuture<Collection<CloudPlayer>> playersAsync();

  /**
   * Gets all unique ids of the players supplied by this player provider asynchronously.
   *
   * @return a task containing all supplied unique ids.
   */
  @NonNull
  CompletableFuture<Collection<UUID>> uniqueIdsAsync();

  /**
   * Gets all player names for all players supplied by this player provider asynchronously.
   *
   * @return a task containing all supplied player names.
   */
  @NonNull
  CompletableFuture<Collection<String>> namesAsync();

  /**
   * Gets the count of supplied players by this player provider asynchronously.
   *
   * @return a task containing the amount of supplied players.
   */
  @NonNull
  CompletableFuture<Integer> countAsync();
}
