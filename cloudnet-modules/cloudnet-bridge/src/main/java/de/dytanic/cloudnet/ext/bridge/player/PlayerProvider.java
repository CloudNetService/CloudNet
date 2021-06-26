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
