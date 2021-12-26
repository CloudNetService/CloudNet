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

package de.dytanic.cloudnet.ext.bridge.platform.bukkit;

import de.dytanic.cloudnet.common.registry.ServicesRegistry;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceHelper;
import de.dytanic.cloudnet.ext.bridge.platform.PlatformBridgeManagement;
import de.dytanic.cloudnet.ext.bridge.player.NetworkPlayerServerInfo;
import de.dytanic.cloudnet.ext.bridge.player.PlayerManager;
import de.dytanic.cloudnet.ext.bridge.player.ServicePlayer;
import de.dytanic.cloudnet.ext.bridge.player.executor.PlayerExecutor;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

final class BukkitBridgeManagement extends PlatformBridgeManagement<Player, NetworkPlayerServerInfo> {

  private static final BiFunction<Player, String, Boolean> PERM_FUNCTION = Permissible::hasPermission;

  private final Plugin plugin;
  private final PlayerExecutor directGlobalExecutor;

  public BukkitBridgeManagement(@NonNull Plugin plugin) {
    super(Wrapper.instance());
    // init fields
    this.plugin = plugin;
    this.directGlobalExecutor = new BukkitDirectPlayerExecutor(
      plugin,
      PlayerExecutor.GLOBAL_UNIQUE_ID,
      Bukkit::getOnlinePlayers);
    // init the bridge properties
    BridgeServiceHelper.MOTD.set(Bukkit.getMotd());
    BridgeServiceHelper.MAX_PLAYERS.set(Bukkit.getMaxPlayers());
  }

  @Override
  public void registerServices(@NonNull ServicesRegistry registry) {
    registry.registerService(PlayerManager.class, "PlayerManager", this.playerManager);
    registry.registerService(PlatformBridgeManagement.class, "BukkitBridgeManagement", this);
  }

  @Override
  public @NonNull ServicePlayer wrapPlayer(@NonNull Player player) {
    return new ServicePlayer(player.getUniqueId(), player.getName());
  }

  @Override
  public @NonNull NetworkPlayerServerInfo createPlayerInformation(@NonNull Player player) {
    return new NetworkPlayerServerInfo(
      player.getUniqueId(),
      player.getName(),
      null,
      new HostAndPort(player.getAddress()),
      this.ownNetworkServiceInfo);
  }

  @Override
  public @NonNull BiFunction<Player, String, Boolean> permissionFunction() {
    return PERM_FUNCTION;
  }

  @Override
  public boolean isOnAnyFallbackInstance(@NonNull Player player) {
    return this.isOnAnyFallbackInstance(this.ownNetworkServiceInfo.serverName(), null, player::hasPermission);
  }

  @Override
  public @NonNull Optional<ServiceInfoSnapshot> fallback(@NonNull Player player) {
    return this.fallback(player, this.ownNetworkServiceInfo.serverName());
  }

  @Override
  public @NonNull Optional<ServiceInfoSnapshot> fallback(@NonNull Player player, @Nullable String currServer) {
    return this.fallback(player.getUniqueId(), currServer, null, player::hasPermission);
  }

  @Override
  public void handleFallbackConnectionSuccess(@NonNull Player player) {
    this.handleFallbackConnectionSuccess(player.getUniqueId());
  }

  @Override
  public void removeFallbackProfile(@NonNull Player player) {
    this.removeFallbackProfile(player.getUniqueId());
  }

  @Override
  public @NonNull PlayerExecutor directPlayerExecutor(@NonNull UUID uniqueId) {
    return uniqueId.equals(PlayerExecutor.GLOBAL_UNIQUE_ID)
      ? this.directGlobalExecutor
      : new BukkitDirectPlayerExecutor(this.plugin, uniqueId, () -> Collections.singleton(Bukkit.getPlayer(uniqueId)));
  }

  @Override
  public void appendServiceInformation(@NonNull ServiceInfoSnapshot snapshot) {
    super.appendServiceInformation(snapshot);
    // append the bukkit specific information
    snapshot.properties().append("Online-Count", Bukkit.getOnlinePlayers().size());
    snapshot.properties().append("Version", Bukkit.getVersion());
    // players
    snapshot.properties().append("Players", Bukkit.getOnlinePlayers().stream()
      .map(this::createPlayerInformation)
      .toList());
  }
}
