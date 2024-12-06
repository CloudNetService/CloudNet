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

package eu.cloudnetservice.modules.bridge.platform.minestom;

import eu.cloudnetservice.modules.bridge.platform.PlatformPlayerExecutorAdapter;
import eu.cloudnetservice.modules.bridge.player.executor.ServerSelectorType;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.minestom.server.command.CommandManager;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

final class MinestomDirectPlayerExecutor extends PlatformPlayerExecutorAdapter<Player> {

  private final Supplier<MinestomPermissionChecker> permissionChecker;
  private final CommandManager commandManager;

  public MinestomDirectPlayerExecutor(
    @NonNull Supplier<MinestomPermissionChecker> permissionChecker,
    @NonNull CommandManager commandManager,
    @NonNull UUID uniqueId,
    @NonNull Supplier<? extends Collection<? extends Player>> supplier
  ) {
    super(uniqueId, supplier);
    this.permissionChecker = permissionChecker;
    this.commandManager = commandManager;
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
    this.forEach(player -> player.kick(message));
  }

  @Override
  public void sendTitle(@NonNull Title title) {
    this.forEach(player -> player.showTitle(title));
  }

  @Override
  public void sendChatMessage(@NonNull Component message, @Nullable String permission) {
    this.forEach(player -> {
      if (permission == null || this.permissionChecker.get().hasPermission(player, permission)) {
        player.sendMessage(message);
      }
    });
  }

  @Override
  public void sendPluginMessage(@NonNull String key, byte[] data) {
    this.forEach(player -> player.sendPluginMessage(key, data));
  }

  @Override
  public void spoofCommandExecution(@NonNull String command, boolean redirectToServer) {
    this.forEach(player -> this.commandManager.execute(player, command));
  }
}
