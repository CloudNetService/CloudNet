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

package eu.cloudnetservice.modules.bridge.node.player;

import eu.cloudnetservice.modules.bridge.player.CloudPlayer;
import eu.cloudnetservice.modules.bridge.player.PlayerProvider;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;

final class NodePlayerProvider implements PlayerProvider {

  private final Supplier<Stream<CloudPlayer>> playerSupplier;

  public NodePlayerProvider(@NonNull Supplier<Stream<CloudPlayer>> playerSupplier) {
    this.playerSupplier = playerSupplier;
  }

  @Override
  public @NonNull Collection<CloudPlayer> players() {
    return this.playerSupplier.get().toList();
  }

  @Override
  public @NonNull Collection<UUID> uniqueIds() {
    return this.playerSupplier.get().map(CloudPlayer::uniqueId).collect(Collectors.toSet());
  }

  @Override
  public @NonNull Collection<String> names() {
    return this.playerSupplier.get().map(CloudPlayer::name).collect(Collectors.toSet());
  }

  @Override
  public int count() {
    return (int) this.playerSupplier.get().count();
  }
}
