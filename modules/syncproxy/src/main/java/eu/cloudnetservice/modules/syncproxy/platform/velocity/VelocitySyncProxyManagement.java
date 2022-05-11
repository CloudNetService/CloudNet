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

package eu.cloudnetservice.modules.syncproxy.platform.velocity;

import static eu.cloudnetservice.ext.adventure.AdventureSerializerUtil.serialize;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.modules.syncproxy.platform.PlatformSyncProxyManagement;
import java.util.Collection;
import java.util.UUID;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class VelocitySyncProxyManagement extends PlatformSyncProxyManagement<Player> {

  private final ProxyServer proxyServer;

  public VelocitySyncProxyManagement(@NonNull ProxyServer proxyServer) {
    this.proxyServer = proxyServer;
    this.init();
  }

  @Override
  public void registerService(@NonNull ServiceRegistry registry) {
    registry.registerProvider(PlatformSyncProxyManagement.class, "VelocitySyncProxyManagement", this);
  }

  @Override
  public void unregisterService(@NonNull ServiceRegistry registry) {
    registry.unregisterProvider(PlatformSyncProxyManagement.class, "VelocitySyncProxyManagement");
  }

  @Override
  public @NonNull Collection<Player> onlinePlayers() {
    return this.proxyServer.getAllPlayers();
  }

  @Override
  public @NonNull String playerName(@NonNull Player player) {
    return player.getUsername();
  }

  @Override
  public @NonNull UUID playerUniqueId(@NonNull Player player) {
    return player.getUniqueId();
  }

  @Override
  public void playerTabList(@NonNull Player player, @Nullable String header, @Nullable String footer) {
    if (header == null || footer == null) {
      player.getTabList().clearHeaderAndFooter();
    } else {
      player.sendPlayerListHeaderAndFooter(
        serialize(this.replaceTabPlaceholder(header, player)),
        serialize(this.replaceTabPlaceholder(footer, player)));
    }
  }

  @Override
  public void disconnectPlayer(@NonNull Player player, @NonNull String message) {
    player.disconnect(serialize(message));
  }

  @Override
  public void messagePlayer(@NonNull Player player, @Nullable String message) {
    if (message != null) {
      player.sendMessage(serialize(message));
    }
  }

  @Override
  public boolean checkPlayerPermission(@NonNull Player player, @NonNull String permission) {
    return player.hasPermission(permission);
  }

  private @NonNull String replaceTabPlaceholder(@NonNull String input, @NonNull Player player) {
    var server = player.getCurrentServer()
      .map(serverConnection -> serverConnection.getServerInfo().getName())
      .orElse("UNAVAILABLE");

    return input
      .replace("%ping%", String.valueOf(player.getPing()))
      .replace("%server%", server);
  }
}
