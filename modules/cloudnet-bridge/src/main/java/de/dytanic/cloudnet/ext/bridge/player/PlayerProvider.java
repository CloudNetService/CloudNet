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
import java.util.Collection;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * This interface extends the player access of the {@link IPlayerManager} This {@link PlayerProvider} can be global, but
 * for certain tasks and groups too
 */
public interface PlayerProvider {

  /**
   * @return all players as {@link ICloudPlayer}
   */
  @NotNull
  Collection<? extends ICloudPlayer> asPlayers();

  /**
   * @return the uniqueIds of all players
   */
  @NotNull
  Collection<UUID> asUUIDs();

  /**
   * @return the names of all players
   */
  @NotNull
  Collection<String> asNames();

  /**
   * @return the player count
   */
  int count();

  /**
   * @return all players as {@link ICloudPlayer}
   */
  @NotNull
  ITask<Collection<? extends ICloudPlayer>> asPlayersAsync();

  /**
   * @return the uniqueIds of all players
   */
  @NotNull
  ITask<Collection<UUID>> asUUIDsAsync();

  /**
   * @return the names of all players
   */
  @NotNull
  ITask<Collection<String>> asNamesAsync();

  /**
   * @return the player count
   */
  @NotNull
  ITask<Integer> countAsync();

}
