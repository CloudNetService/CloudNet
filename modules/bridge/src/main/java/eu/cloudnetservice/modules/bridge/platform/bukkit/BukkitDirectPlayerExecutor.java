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

package eu.cloudnetservice.modules.bridge.platform.bukkit;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

import eu.cloudnetservice.modules.bridge.platform.PlatformPlayerExecutorAdapter;
import eu.cloudnetservice.modules.bridge.player.executor.ServerSelectorType;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

final class BukkitDirectPlayerExecutor extends PlatformPlayerExecutorAdapter {

  private final Plugin plugin;
  private final UUID targetUniqueId;
  private final Supplier<Collection<? extends Player>> playerSupplier;

  public BukkitDirectPlayerExecutor(
    @NonNull Plugin plugin,
    @NonNull UUID target,
    @NonNull Supplier<Collection<? extends Player>> playerSupplier
  ) {
    this.plugin = plugin;
    this.targetUniqueId = target;
    this.playerSupplier = playerSupplier;
  }

  @Override
  public @NonNull UUID uniqueId() {
    return this.targetUniqueId;
  }

  @Override
  public void connect(@NonNull String serviceName) {
    // no-op
  }

  @Override
  public void connectSelecting(@NonNull ServerSelectorType selectorType) {
    // no-op
  }

  @Override
  public void connectToFallback() {
    // no-op
  }

  @Override
  public void connectToGroup(@NonNull String group, @NonNull ServerSelectorType selectorType) {
    // no-op
  }

  @Override
  public void connectToTask(@NonNull String task, @NonNull ServerSelectorType selectorType) {
    // no-op
  }

  @Override
  public void kick(@NonNull Component message) {
    this.playerSupplier.get().forEach(player -> player.kickPlayer(legacySection().serialize(message)));
  }

  @Override
  protected void sendTitle(@NonNull Component title, @NonNull Component subtitle, int fadeIn, int stay, int fadeOut) {
    this.playerSupplier.get().forEach(player -> player.sendTitle(
      legacySection().serialize(title),
      legacySection().serialize(subtitle)));
  }

  @Override
  public void sendMessage(@NonNull Component message) {
    this.playerSupplier.get().forEach(player -> player.sendMessage(legacySection().serialize(message)));
  }

  @Override
  public void sendChatMessage(@NonNull Component message, @Nullable String permission) {
    this.playerSupplier.get().forEach(player -> {
      if (permission == null || player.hasPermission(permission)) {
        player.sendMessage(legacySection().serialize(message));
      }
    });
  }

  @Override
  public void sendPluginMessage(@NonNull String tag, byte[] data) {
    this.playerSupplier.get().forEach(player -> player.sendPluginMessage(this.plugin, tag, data));
  }

  @Override
  public void spoofChatInput(@NonNull String command) {
    this.playerSupplier.get().forEach(player -> player.chat(command));
  }
}
