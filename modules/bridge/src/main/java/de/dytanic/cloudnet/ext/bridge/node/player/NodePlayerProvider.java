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

package de.dytanic.cloudnet.ext.bridge.node.player;

import de.dytanic.cloudnet.ext.bridge.player.CloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.PlayerProvider;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;

final class NodePlayerProvider implements PlayerProvider {

  private final Supplier<Stream<? extends CloudPlayer>> playerSupplier;

  public NodePlayerProvider(@NonNull Supplier<Stream<? extends CloudPlayer>> playerSupplier) {
    this.playerSupplier = playerSupplier;
  }

  @Override
  public @NonNull Collection<? extends CloudPlayer> players() {
    return this.playerSupplier.get().collect(Collectors.toList());
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
