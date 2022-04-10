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

package eu.cloudnetservice.modules.bridge.platform;

import eu.cloudnetservice.driver.network.rpc.RPC;
import eu.cloudnetservice.driver.network.rpc.RPCSender;
import eu.cloudnetservice.modules.bridge.player.CloudPlayer;
import eu.cloudnetservice.modules.bridge.player.PlayerProvider;
import java.util.Collection;
import java.util.UUID;
import lombok.NonNull;

final class PlatformPlayerProvider implements PlayerProvider {

  private final RPC baseRPC;
  private final RPCSender sender;

  public PlatformPlayerProvider(@NonNull RPC baseRPC) {
    this.baseRPC = baseRPC;
    this.sender = baseRPC.sender().factory().providerForClass(null, PlayerProvider.class);
  }

  @Override
  public @NonNull Collection<? extends CloudPlayer> players() {
    return this.baseRPC.join(this.sender.invokeMethod("players")).fireSync();
  }

  @Override
  public @NonNull Collection<UUID> uniqueIds() {
    return this.baseRPC.join(this.sender.invokeMethod("uniqueIds")).fireSync();
  }

  @Override
  public @NonNull Collection<String> names() {
    return this.baseRPC.join(this.sender.invokeMethod("names")).fireSync();
  }

  @Override
  public int count() {
    return this.baseRPC.join(this.sender.invokeMethod("count")).fireSync();
  }
}
