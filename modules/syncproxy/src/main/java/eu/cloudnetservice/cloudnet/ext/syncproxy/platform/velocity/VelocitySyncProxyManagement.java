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

package eu.cloudnetservice.cloudnet.ext.syncproxy.platform.velocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.dytanic.cloudnet.common.registry.IServicesRegistry;
import eu.cloudnetservice.cloudnet.ext.syncproxy.platform.PlatformSyncProxyManagement;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class VelocitySyncProxyManagement extends PlatformSyncProxyManagement<Player> {

  private final ProxyServer proxyServer;
  private final VelocitySyncProxyPlugin plugin;

  public VelocitySyncProxyManagement(@NotNull ProxyServer proxyServer, @NotNull VelocitySyncProxyPlugin plugin) {
    this.proxyServer = proxyServer;
    this.plugin = plugin;
    this.init();
  }

  @Override
  public void registerService(@NotNull IServicesRegistry registry) {
    registry.registerService(PlatformSyncProxyManagement.class, "Velocity", this);
  }

  @Override
  public void unregisterService(@NotNull IServicesRegistry registry) {
    registry.unregisterService(PlatformSyncProxyManagement.class, "Velocity");
  }

  @Override
  public void schedule(@NotNull Runnable runnable, long time, @NotNull TimeUnit unit) {
    this.proxyServer.getScheduler().buildTask(this.plugin, runnable)
      .delay(time, unit)
      .schedule();
  }

  @Override
  public @NotNull Collection<Player> getOnlinePlayers() {
    return this.proxyServer.getAllPlayers();
  }

  @Override
  public @NotNull String getPlayerName(@NotNull Player player) {
    return player.getUsername();
  }

  @Override
  public @NotNull UUID getPlayerUniqueId(@NotNull Player player) {
    return player.getUniqueId();
  }

  @Override
  public void setPlayerTabList(@NotNull Player player, @Nullable String header, @Nullable String footer) {
    if (header == null || footer == null) {
      player.getTabList().clearHeaderAndFooter();
      return;
    }

    player.sendPlayerListHeaderAndFooter(
      this.asComponent(this.replaceTabPlaceholder(header, player)),
      this.asComponent(this.replaceTabPlaceholder(footer, player)));
  }

  @Override
  public void disconnectPlayer(@NotNull Player player, @NotNull String message) {
    player.disconnect(this.asComponent(message));
  }

  @Override
  public void messagePlayer(@NotNull Player player, @Nullable String message) {
    if (message == null) {
      return;
    }

    player.sendMessage(this.asComponent(message));
  }

  @Override
  public boolean checkPlayerPermission(@NotNull Player player, @NotNull String permission) {
    return player.hasPermission(permission);
  }

  @Contract("null -> null; !null -> !null")
  private @Nullable TextComponent asComponent(@Nullable String message) {
    if (message == null) {
      return null;
    }

    return PlainTextComponentSerializer.plainText().deserialize(message);
  }

  private String replaceTabPlaceholder(@NotNull String input, @NotNull Player player) {
    String server = player.getCurrentServer()
      .map(serverConnection -> serverConnection.getServerInfo().getName())
      .orElse("");

    return input
      .replace("%ping%", String.valueOf(player.getPing()))
      .replace("%server%", server);
  }
}
