/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.bridge.platform;

import eu.cloudnetservice.driver.network.rpc.RPCSender;
import eu.cloudnetservice.driver.network.rpc.generation.GenerationContext;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import eu.cloudnetservice.modules.bridge.player.PlayerProvider;
import eu.cloudnetservice.modules.bridge.player.executor.PlayerExecutor;
import java.util.UUID;
import lombok.NonNull;

abstract class PlatformPlayerManager implements PlayerManager {

  private final RPCSender sender;
  private final PlayerProvider allPlayers;
  private final PlayerExecutor globalPlayerExecutor;

  public PlatformPlayerManager(@NonNull RPCSender sender) {
    this.sender = sender;
    // init the static player utils
    this.globalPlayerExecutor = this.playerExecutor(PlayerExecutor.GLOBAL_UNIQUE_ID);
    this.allPlayers = sender.factory().generateRPCChainBasedApi(
      sender,
      "onlinePlayers",
      PlayerProvider.class,
      GenerationContext.forClass(PlayerProvider.class).build()
    ).newInstance();
  }

  @Override
  public @NonNull PlayerProvider onlinePlayers() {
    return this.allPlayers;
  }

  @Override
  public @NonNull PlayerExecutor globalPlayerExecutor() {
    return this.globalPlayerExecutor;
  }

  @Override
  public @NonNull PlayerExecutor playerExecutor(@NonNull UUID uniqueId) {
    return this.sender.factory().generateRPCChainBasedApi(
      this.sender,
      PlayerExecutor.class,
      GenerationContext.forClass(PlatformPlayerExecutor.class).build()
    ).newInstance(uniqueId);
  }
}
