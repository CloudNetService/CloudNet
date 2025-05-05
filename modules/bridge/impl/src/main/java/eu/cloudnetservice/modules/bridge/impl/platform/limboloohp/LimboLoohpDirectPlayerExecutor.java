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

package eu.cloudnetservice.modules.bridge.impl.platform.limboloohp;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

import com.loohp.limbo.player.Player;
import com.loohp.limbo.plugins.LimboPlugin;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.modules.bridge.impl.platform.PlatformPlayerExecutorAdapter;
import eu.cloudnetservice.modules.bridge.player.executor.ServerSelectorType;
import java.io.IOException;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.NonNull;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class LimboLoohpDirectPlayerExecutor extends PlatformPlayerExecutorAdapter<Player> {

  private final LimboPlugin plugin;
  private static final Logger LOGGER = LoggerFactory.getLogger(LimboLoohpDirectPlayerExecutor.class);

  public LimboLoohpDirectPlayerExecutor(
    @NonNull LimboPlugin plugin,
    @NonNull UUID target,
    @NonNull Supplier<Collection<? extends Player>> playerSupplier
  ) {
    super(target, playerSupplier);
    this.plugin = plugin;
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
    this.plugin.getServer().getScheduler().runTask(
      this.plugin,
      () -> this.forEach(player -> player.disconnect(legacySection().serialize(message))));
  }

  @Override
  protected void sendTitle(@NonNull Component title, @NonNull Component subtitle, int fadeIn, int stay, int fadeOut) {
    this.forEach(player -> player.setTitleSubTitle(
      legacySection().serialize(title),
      legacySection().serialize(subtitle),
      fadeIn,
      stay,
      fadeOut)
    );
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
    this.forEach(player -> {
      try {
        player.sendPluginMessage(Key.key(key), data);
      } catch (IOException e) {
        LOGGER.error(I18n.i18n().translate("module-bridge-plugin-message-sending-failed", key), e);
      }
    });
  }

  @Override
  public void spoofCommandExecution(@NonNull String command, boolean redirectToServer) {
    var server = this.plugin.getServer();
    server.getScheduler().runTask(this.plugin, () -> this.forEach(player -> server.dispatchCommand(player, command)));
  }
}
