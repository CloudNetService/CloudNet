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

package eu.cloudnetservice.modules.bridge.platform.fabric;

import eu.cloudnetservice.cloudnet.common.registry.ServicesRegistry;
import eu.cloudnetservice.cloudnet.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.cloudnet.wrapper.Wrapper;
import eu.cloudnetservice.modules.bridge.BridgeServiceHelper;
import eu.cloudnetservice.modules.bridge.platform.PlatformBridgeManagement;
import eu.cloudnetservice.modules.bridge.platform.fabric.util.BridgedServer;
import eu.cloudnetservice.modules.bridge.player.NetworkPlayerServerInfo;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import eu.cloudnetservice.modules.bridge.player.ServicePlayer;
import eu.cloudnetservice.modules.bridge.player.executor.PlayerExecutor;
import eu.cloudnetservice.modules.bridge.util.BridgeHostAndPortUtil;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import lombok.NonNull;
import net.minecraft.SharedConstants;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

public final class FabricBridgeManagement
  extends PlatformBridgeManagement<ServerPlayerEntity, NetworkPlayerServerInfo> {

  private final BridgedServer server;
  private final PlayerExecutor directGlobalExecutor;

  public FabricBridgeManagement(@NonNull BridgedServer server) {
    super(Wrapper.instance());
    // field init
    this.server = server;
    this.directGlobalExecutor = new FabricDirectPlayerExecutor(PlayerExecutor.GLOBAL_UNIQUE_ID, server::players);
    // init the bridge properties
    BridgeServiceHelper.MOTD.set(server.motd());
    BridgeServiceHelper.MAX_PLAYERS.set(server.maxPlayers());
  }

  @Override
  public void registerServices(@NonNull ServicesRegistry registry) {
    registry.registerService(PlayerManager.class, "PlayerManager", this.playerManager);
    registry.registerService(PlatformBridgeManagement.class, "FabricBridgeManagement", this);
  }

  @Override
  public @NonNull ServicePlayer wrapPlayer(@NonNull ServerPlayerEntity player) {
    return new ServicePlayer(player.getUuid(), player.getEntityName());
  }

  @Override
  public @NonNull NetworkPlayerServerInfo createPlayerInformation(@NonNull ServerPlayerEntity player) {
    return new NetworkPlayerServerInfo(
      player.getUuid(),
      player.getEntityName(),
      null,
      BridgeHostAndPortUtil.fromSocketAddress(player.networkHandler.getConnection().getAddress()),
      this.ownNetworkServiceInfo);
  }

  @Override
  public @NonNull BiFunction<ServerPlayerEntity, String, Boolean> permissionFunction() {
    return (player, perm) -> true;
  }

  @Override
  public boolean isOnAnyFallbackInstance(@NonNull ServerPlayerEntity player) {
    return this.isOnAnyFallbackInstance(this.ownNetworkServiceInfo.serverName(), null, perm -> true);
  }

  @Override
  public @NonNull Optional<ServiceInfoSnapshot> fallback(@NonNull ServerPlayerEntity player) {
    return this.fallback(player, this.ownNetworkServiceInfo.serverName());
  }

  @Override
  public @NonNull Optional<ServiceInfoSnapshot> fallback(
    @NonNull ServerPlayerEntity player,
    @Nullable String currServer
  ) {
    return this.fallback(player.getUuid(), currServer, null, perm -> true);
  }

  @Override
  public void handleFallbackConnectionSuccess(@NonNull ServerPlayerEntity player) {
    this.handleFallbackConnectionSuccess(player.getUuid());
  }

  @Override
  public void removeFallbackProfile(@NonNull ServerPlayerEntity player) {
    this.removeFallbackProfile(player.getUuid());
  }

  @Override
  public @NonNull PlayerExecutor directPlayerExecutor(@NonNull UUID uniqueId) {
    return uniqueId.equals(PlayerExecutor.GLOBAL_UNIQUE_ID)
      ? this.directGlobalExecutor
      : new FabricDirectPlayerExecutor(uniqueId, () -> Collections.singleton(this.server.player(uniqueId)));
  }

  @Override
  public void appendServiceInformation(@NonNull ServiceInfoSnapshot snapshot) {
    super.appendServiceInformation(snapshot);
    // append the fabric specific information
    snapshot.properties().append("Online-Count", this.server.playerCount());
    snapshot.properties().append("Version", SharedConstants.getGameVersion().getName());
    // players
    snapshot.properties().append("Players", this.server.players().stream()
      .map(this::createPlayerInformation)
      .toList());
  }
}
