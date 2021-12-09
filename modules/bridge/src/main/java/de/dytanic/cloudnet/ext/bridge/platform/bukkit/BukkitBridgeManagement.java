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

import de.dytanic.cloudnet.common.registry.IServicesRegistry;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceHelper;
import de.dytanic.cloudnet.ext.bridge.platform.PlatformBridgeManagement;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import de.dytanic.cloudnet.ext.bridge.player.NetworkPlayerServerInfo;
import de.dytanic.cloudnet.ext.bridge.player.ServicePlayer;
import de.dytanic.cloudnet.ext.bridge.player.executor.PlayerExecutor;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class BukkitBridgeManagement extends PlatformBridgeManagement<Player, NetworkPlayerServerInfo> {

  private static final BiFunction<Player, String, Boolean> PERM_FUNCTION = Permissible::hasPermission;

  private final Plugin plugin;
  private final PlayerExecutor directGlobalExecutor;

  public BukkitBridgeManagement(@NotNull Plugin plugin) {
    super(Wrapper.getInstance());
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
  public void registerServices(@NotNull IServicesRegistry registry) {
    registry.registerService(IPlayerManager.class, "PlayerManager", this.playerManager);
    registry.registerService(PlatformBridgeManagement.class, "BukkitBridgeManagement", this);
  }

  @Override
  public @NotNull ServicePlayer wrapPlayer(@NotNull Player player) {
    return new ServicePlayer(player.getUniqueId(), player.getName());
  }

  @Override
  public @NotNull NetworkPlayerServerInfo createPlayerInformation(@NotNull Player player) {
    return new NetworkPlayerServerInfo(
      player.getUniqueId(),
      player.getName(),
      null,
      new HostAndPort(player.getAddress()),
      this.ownNetworkServiceInfo);
  }

  @Override
  public @NotNull BiFunction<Player, String, Boolean> getPermissionFunction() {
    return PERM_FUNCTION;
  }

  @Override
  public boolean isOnAnyFallbackInstance(@NotNull Player player) {
    return this.isOnAnyFallbackInstance(this.ownNetworkServiceInfo.getServerName(), null, player::hasPermission);
  }

  @Override
  public @NotNull Optional<ServiceInfoSnapshot> getFallback(@NotNull Player player) {
    return this.getFallback(player, this.ownNetworkServiceInfo.getServerName());
  }

  @Override
  public @NotNull Optional<ServiceInfoSnapshot> getFallback(@NotNull Player player, @Nullable String currServer) {
    return this.getFallback(player.getUniqueId(), currServer, null, player::hasPermission);
  }

  @Override
  public void handleFallbackConnectionSuccess(@NotNull Player player) {
    this.handleFallbackConnectionSuccess(player.getUniqueId());
  }

  @Override
  public void removeFallbackProfile(@NotNull Player player) {
    this.removeFallbackProfile(player.getUniqueId());
  }

  @Override
  public @NotNull PlayerExecutor getDirectPlayerExecutor(@NotNull UUID uniqueId) {
    return uniqueId.equals(PlayerExecutor.GLOBAL_UNIQUE_ID)
      ? this.directGlobalExecutor
      : new BukkitDirectPlayerExecutor(this.plugin, uniqueId, () -> Collections.singleton(Bukkit.getPlayer(uniqueId)));
  }

  @Override
  public void appendServiceInformation(@NotNull ServiceInfoSnapshot snapshot) {
    super.appendServiceInformation(snapshot);
    // append the bukkit specific information
    snapshot.getProperties().append("Online-Count", Bukkit.getOnlinePlayers().size());
    snapshot.getProperties().append("Version", Bukkit.getVersion());
    // players
    snapshot.getProperties().append("Players", Bukkit.getOnlinePlayers().stream()
      .map(this::createPlayerInformation)
      .collect(Collectors.toList()));
  }
}
