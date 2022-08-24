/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

import eu.cloudnetservice.common.concurrent.Task;
import eu.cloudnetservice.driver.network.rpc.annotation.RPCValidation;
import java.util.Collection;
import java.util.UUID;
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
@RPCValidation
public interface PlayerProvider {

  /**
   * Gets all players supplied by this player provider as cloud players.
   *
   * @return all supplied cloud players.
   */
  @NonNull Collection<CloudPlayer> players();

  /**
   * Gets all unique ids of the players supplied by this player provider.
   *
   * @return all supplied unique ids.
   */
  @NonNull Collection<UUID> uniqueIds();

  /**
   * Gets all player names for all players supplied by this player provider.
   *
   * @return all supplied player names.
   */
  @NonNull Collection<String> names();

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
  default @NonNull Task<Collection<CloudPlayer>> playersAsync() {
    return Task.supply(this::players);
  }

  /**
   * Gets all unique ids of the players supplied by this player provider asynchronously.
   *
   * @return a task containing all supplied unique ids.
   */
  default @NonNull Task<Collection<UUID>> uniqueIdsAsync() {
    return Task.supply(this::uniqueIds);
  }

  /**
   * Gets all player names for all players supplied by this player provider asynchronously.
   *
   * @return a task containing all supplied player names.
   */
  default @NonNull Task<Collection<String>> namesAsync() {
    return Task.supply(this::names);
  }

  /**
   * Gets the count of supplied players by this player provider asynchronously.
   *
   * @return a task containing the amount of supplied players.
   */
  default @NonNull Task<Integer> countAsync() {
    return Task.supply(this::count);
  }
}
