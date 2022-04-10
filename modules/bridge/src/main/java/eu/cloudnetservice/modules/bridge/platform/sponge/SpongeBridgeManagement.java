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

package eu.cloudnetservice.modules.bridge.platform.sponge;

import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.bridge.BridgeServiceHelper;
import eu.cloudnetservice.modules.bridge.platform.PlatformBridgeManagement;
import eu.cloudnetservice.modules.bridge.player.NetworkPlayerServerInfo;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import eu.cloudnetservice.modules.bridge.player.ServicePlayer;
import eu.cloudnetservice.modules.bridge.player.executor.PlayerExecutor;
import eu.cloudnetservice.modules.bridge.util.BridgeHostAndPortUtil;
import eu.cloudnetservice.wrapper.Wrapper;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import lombok.NonNull;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.service.permission.Subject;

final class SpongeBridgeManagement extends PlatformBridgeManagement<ServerPlayer, NetworkPlayerServerInfo> {

  private static final BiFunction<ServerPlayer, String, Boolean> PERM_FUNCTION = Subject::hasPermission;

  private final PlayerExecutor directGlobalExecutor;

  public SpongeBridgeManagement() {
    super(Wrapper.instance());
    // init fields
    this.directGlobalExecutor = new SpongeDirectPlayerExecutor(
      PlayerExecutor.GLOBAL_UNIQUE_ID,
      () -> Sponge.server().onlinePlayers());
    // init the bridge properties
    BridgeServiceHelper.MAX_PLAYERS.set(Sponge.server().maxPlayers());
    BridgeServiceHelper.MOTD.set(LegacyComponentSerializer.legacySection().serialize(Sponge.server().motd()));
  }

  @Override
  public void registerServices(@NonNull ServiceRegistry registry) {
    registry.registerProvider(PlayerManager.class, "PlayerManager", this.playerManager);
    registry.registerProvider(PlatformBridgeManagement.class, "SpongeBridgeManagement", this);
  }

  @Override
  public @NonNull ServicePlayer wrapPlayer(@NonNull ServerPlayer player) {
    return new ServicePlayer(player.uniqueId(), player.name());
  }

  @Override
  public @NonNull NetworkPlayerServerInfo createPlayerInformation(@NonNull ServerPlayer player) {
    return new NetworkPlayerServerInfo(
      player.uniqueId(),
      player.name(),
      null,
      BridgeHostAndPortUtil.fromSocketAddress(player.connection().address()),
      this.ownNetworkServiceInfo);
  }

  @Override
  public @NonNull BiFunction<ServerPlayer, String, Boolean> permissionFunction() {
    return PERM_FUNCTION;
  }

  @Override
  public boolean isOnAnyFallbackInstance(@NonNull ServerPlayer player) {
    return this.isOnAnyFallbackInstance(this.ownNetworkServiceInfo.serverName(), null, player::hasPermission);
  }

  @Override
  public @NonNull Optional<ServiceInfoSnapshot> fallback(@NonNull ServerPlayer player) {
    return this.fallback(player, this.ownNetworkServiceInfo.serverName());
  }

  @Override
  public @NonNull Optional<ServiceInfoSnapshot> fallback(@NonNull ServerPlayer player, @Nullable String currServer) {
    return this.fallback(player.uniqueId(), currServer, null, player::hasPermission);
  }

  @Override
  public void handleFallbackConnectionSuccess(@NonNull ServerPlayer player) {
    this.handleFallbackConnectionSuccess(player.uniqueId());
  }

  @Override
  public void removeFallbackProfile(@NonNull ServerPlayer player) {
    this.removeFallbackProfile(player.uniqueId());
  }

  @Override
  public @NonNull PlayerExecutor directPlayerExecutor(@NonNull UUID uniqueId) {
    return uniqueId.equals(PlayerExecutor.GLOBAL_UNIQUE_ID)
      ? this.directGlobalExecutor
      : new SpongeDirectPlayerExecutor(
        uniqueId,
        () -> Collections.singleton(Sponge.server().player(uniqueId).orElse(null)));
  }

  @Override
  public void appendServiceInformation(@NonNull ServiceInfoSnapshot snapshot) {
    super.appendServiceInformation(snapshot);
    // append the bukkit specific information
    snapshot.properties().append("Online-Count", Sponge.server().onlinePlayers().size());
    snapshot.properties().append("Version", Sponge.platform().minecraftVersion().name());
    // players
    snapshot.properties().append("Players", Sponge.server().onlinePlayers().stream()
      .map(this::createPlayerInformation)
      .toList());
  }
}
