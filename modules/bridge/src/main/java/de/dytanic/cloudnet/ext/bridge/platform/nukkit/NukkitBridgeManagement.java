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

package de.dytanic.cloudnet.ext.bridge.platform.nukkit;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.permission.Permissible;
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
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

final class NukkitBridgeManagement extends PlatformBridgeManagement<Player, NetworkPlayerServerInfo> {

  private static final BiFunction<Player, String, Boolean> PERM_FUNCTION = Permissible::hasPermission;

  private final PlayerExecutor globalPlayerExecutor;

  public NukkitBridgeManagement(@NonNull Wrapper wrapper) {
    super(wrapper);
    // init fields
    this.globalPlayerExecutor = new NukkitDirectPlayerExecutor(
      PlayerExecutor.GLOBAL_UNIQUE_ID,
      () -> Server.getInstance().getOnlinePlayers().values());
    // init the bridge properties
    BridgeServiceHelper.MOTD.set(Server.getInstance().getMotd());
    BridgeServiceHelper.MAX_PLAYERS.set(Server.getInstance().getMaxPlayers());
  }

  @Override
  public void registerServices(@NonNull IServicesRegistry registry) {
    registry.registerService(IPlayerManager.class, "PlayerManager", this.playerManager);
    registry.registerService(PlatformBridgeManagement.class, "NukkitBridgeManagement", this);
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
      player.getLoginChainData().getXUID(),
      new HostAndPort(player.getSocketAddress()),
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
      ? this.globalPlayerExecutor
      : new NukkitDirectPlayerExecutor(
        uniqueId,
        () -> Collections.singleton(Server.getInstance().getPlayer(uniqueId).orElse(null)));
  }

  @Override
  public void appendServiceInformation(@NonNull ServiceInfoSnapshot snapshot) {
    super.appendServiceInformation(snapshot);
    // append the bukkit specific information
    snapshot.properties().append("Online-Count", Server.getInstance().getOnlinePlayers().size());
    snapshot.properties().append("Version", Server.getInstance().getVersion());
    // players
    snapshot.properties().append("Players", Server.getInstance().getOnlinePlayers().values().stream()
      .map(this::createPlayerInformation)
      .toList());
  }
}
