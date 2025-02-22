/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.bridge.impl.platform.nukkit;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

import cn.nukkit.Player;
import cn.nukkit.Server;
import eu.cloudnetservice.modules.bridge.impl.platform.PlatformPlayerExecutorAdapter;
import eu.cloudnetservice.modules.bridge.player.executor.ServerSelectorType;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

final class NukkitDirectPlayerExecutor extends PlatformPlayerExecutorAdapter<Player> {

  private final Server server;

  public NukkitDirectPlayerExecutor(
    @NonNull Server server,
    @NonNull UUID targetUniqueId,
    @NonNull Supplier<Collection<? extends Player>> playerSupplier
  ) {
    super(targetUniqueId, playerSupplier);
    this.server = server;
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
    this.forEach(player -> player.kick(legacySection().serialize(message)));
  }

  @Override
  protected void sendTitle(@NonNull Component title, @NonNull Component subtitle, int fadeIn, int stay, int fadeOut) {
    this.forEach(player -> player.sendTitle(
      legacySection().serialize(title),
      legacySection().serialize(subtitle),
      fadeIn,
      stay,
      fadeOut));
  }

  @Override
  public void sendChatMessage(@NonNull Component message, @Nullable String permission) {
    this.forEach(player -> {
      if (permission == null || player.hasPermission(permission)) {
        player.sendMessage(legacySection().serialize(message));
      }
    });
  }

  @Override
  public void sendPluginMessage(@NonNull String key, byte[] data) {
    // no-op
  }

  @Override
  public void spoofCommandExecution(@NonNull String command, boolean redirectToServer) {
    this.forEach(player -> this.server.dispatchCommand(player, command));
  }
}
