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

import eu.cloudnetservice.cloudnet.common.concurrent.CompletableTask;
import eu.cloudnetservice.cloudnet.common.concurrent.Task;
import eu.cloudnetservice.cloudnet.driver.network.rpc.annotation.RPCValidation;
import java.util.Collection;
import java.util.UUID;
import lombok.NonNull;

/**
 * This interface extends the player access of the {@link PlayerManager} This {@link PlayerProvider} can be global, but
 * for certain tasks and groups too
 */
@RPCValidation
public interface PlayerProvider {

  /**
   * @return all players as {@link CloudPlayer}
   */
  @NonNull Collection<? extends CloudPlayer> players();

  /**
   * @return the uniqueIds of all players
   */
  @NonNull Collection<UUID> uniqueIds();

  /**
   * @return the names of all players
   */
  @NonNull Collection<String> names();

  /**
   * @return the player count
   */
  int count();

  /**
   * @return all players as {@link CloudPlayer}
   */
  default @NonNull Task<Collection<? extends CloudPlayer>> playersAsync() {
    return CompletableTask.supply(this::players);
  }

  /**
   * @return the uniqueIds of all players
   */
  default @NonNull Task<Collection<UUID>> uniqueIdsAsync() {
    return CompletableTask.supply(this::uniqueIds);
  }

  /**
   * @return the names of all players
   */
  default @NonNull Task<Collection<String>> namesAsync() {
    return CompletableTask.supply(this::names);
  }

  /**
   * @return the player count
   */
  default @NonNull Task<Integer> countAsync() {
    return CompletableTask.supply(this::count);
  }
}
