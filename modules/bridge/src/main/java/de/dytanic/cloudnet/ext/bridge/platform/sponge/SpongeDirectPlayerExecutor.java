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

package de.dytanic.cloudnet.ext.bridge.platform.sponge;

import de.dytanic.cloudnet.ext.bridge.platform.PlatformPlayerExecutorAdapter;
import de.dytanic.cloudnet.ext.bridge.player.executor.ServerSelectorType;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.title.Title;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

final class SpongeDirectPlayerExecutor extends PlatformPlayerExecutorAdapter {

  private final UUID targetUniqueId;
  private final Supplier<Collection<? extends ServerPlayer>> playerSupplier;

  public SpongeDirectPlayerExecutor(
    @NotNull UUID targetUniqueId,
    @NotNull Supplier<Collection<? extends ServerPlayer>> playerSupplier
  ) {
    this.targetUniqueId = targetUniqueId;
    this.playerSupplier = playerSupplier;
  }

  @Override
  public @NotNull UUID getPlayerUniqueId() {
    return this.targetUniqueId;
  }

  @Override
  public void connect(@NotNull String serviceName) {
    // no-op
  }

  @Override
  public void connectSelecting(@NotNull ServerSelectorType selectorType) {
    // no-op
  }

  @Override
  public void connectToFallback() {
    // no-op
  }

  @Override
  public void connectToGroup(@NotNull String group, @NotNull ServerSelectorType selectorType) {
    // no-op
  }

  @Override
  public void connectToTask(@NotNull String task, @NotNull ServerSelectorType selectorType) {
    // no-op
  }

  @Override
  public void kick(@NotNull TextComponent message) {
    // no-op
  }

  @Override
  public void sendTitle(@NotNull Title title) {
    this.playerSupplier.get().forEach(player -> player.showTitle(title));
  }

  @Override
  public void sendMessage(@NotNull TextComponent message) {
    this.playerSupplier.get().forEach(player -> player.sendMessage(message));
  }

  @Override
  public void sendChatMessage(@NotNull TextComponent message, @Nullable String permission) {
    this.playerSupplier.get().forEach(player -> {
      if (permission == null || player.hasPermission(permission)) {
        player.sendMessage(message);
      }
    });
  }

  @Override
  public void sendPluginMessage(@NotNull String tag, byte[] data) {
    // no-op
  }

  @Override
  public void dispatchProxyCommand(@NotNull String command) {
    // no-op
  }
}
