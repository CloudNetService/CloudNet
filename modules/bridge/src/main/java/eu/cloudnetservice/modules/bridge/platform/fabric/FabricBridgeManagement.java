/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.network.rpc.RPCFactory;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.bridge.BridgeServiceHelper;
import eu.cloudnetservice.modules.bridge.platform.PlatformBridgeManagement;
import eu.cloudnetservice.modules.bridge.platform.fabric.util.BridgedServer;
import eu.cloudnetservice.modules.bridge.player.NetworkPlayerServerInfo;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import eu.cloudnetservice.modules.bridge.player.ServicePlayer;
import eu.cloudnetservice.modules.bridge.player.executor.PlayerExecutor;
import eu.cloudnetservice.modules.bridge.util.BridgeHostAndPortUtil;
import eu.cloudnetservice.wrapper.configuration.WrapperConfiguration;
import eu.cloudnetservice.wrapper.holder.ServiceInfoHolder;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import lombok.NonNull;
import net.minecraft.SharedConstants;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class FabricBridgeManagement extends PlatformBridgeManagement<ServerPlayer, NetworkPlayerServerInfo> {

  public static final boolean DISABLE_CLOUDNET_FORWARDING = Boolean.getBoolean("cloudnet.ipforward.disabled");

  private final BridgedServer server;
  private final PlayerExecutor directGlobalExecutor;

  public FabricBridgeManagement(
    @NonNull BridgedServer server,
    @NonNull RPCFactory rpcFactory,
    @NonNull EventManager eventManager,
    @NonNull NetworkClient networkClient,
    @NonNull ServiceTaskProvider taskProvider,
    @NonNull BridgeServiceHelper serviceHelper,
    @NonNull ServiceInfoHolder serviceInfoHolder,
    @NonNull CloudServiceProvider serviceProvider,
    @NonNull WrapperConfiguration wrapperConfiguration
  ) {
    super(
      rpcFactory,
      eventManager,
      networkClient,
      taskProvider,
      serviceHelper,
      serviceInfoHolder,
      serviceProvider,
      wrapperConfiguration);
    // field init
    this.server = server;
    this.directGlobalExecutor = new FabricDirectPlayerExecutor(PlayerExecutor.GLOBAL_UNIQUE_ID, server::players);
    // init the bridge properties
    serviceHelper.motd().set(server.motd());
    serviceHelper.maxPlayers().set(server.maxPlayers());
  }

  @Override
  public void registerServices(@NonNull ServiceRegistry registry) {
    registry.registerProvider(PlayerManager.class, "PlayerManager", this.playerManager);
    registry.registerProvider(PlatformBridgeManagement.class, "FabricBridgeManagement", this);
  }

  @Override
  public @NonNull ServicePlayer wrapPlayer(@NonNull ServerPlayer player) {
    return new ServicePlayer(player.getUUID(), player.getGameProfile().getName());
  }

  @Override
  public @NonNull NetworkPlayerServerInfo createPlayerInformation(@NonNull ServerPlayer player) {
    return new NetworkPlayerServerInfo(
      player.getUUID(),
      player.getGameProfile().getName(),
      null,
      BridgeHostAndPortUtil.fromSocketAddress(player.connection.getConnection().getRemoteAddress()),
      this.ownNetworkServiceInfo);
  }

  @Override
  public @NonNull BiFunction<ServerPlayer, String, Boolean> permissionFunction() {
    return (player, perm) -> true;
  }

  @Override
  public boolean isOnAnyFallbackInstance(@NonNull ServerPlayer player) {
    return this.isOnAnyFallbackInstance(this.ownNetworkServiceInfo.serverName(), null, perm -> true);
  }

  @Override
  public @NonNull Optional<ServiceInfoSnapshot> fallback(@NonNull ServerPlayer player) {
    return this.fallback(player, this.ownNetworkServiceInfo.serverName());
  }

  @Override
  public @NonNull Optional<ServiceInfoSnapshot> fallback(
    @NonNull ServerPlayer player,
    @Nullable String currServer
  ) {
    return this.fallback(player.getUUID(), currServer, null, perm -> true);
  }

  @Override
  public void handleFallbackConnectionSuccess(@NonNull ServerPlayer player) {
    this.handleFallbackConnectionSuccess(player.getUUID());
  }

  @Override
  public void removeFallbackProfile(@NonNull ServerPlayer player) {
    this.removeFallbackProfile(player.getUUID());
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
    snapshot.properties().append("Version", SharedConstants.getCurrentVersion().getName());
    // players
    snapshot.properties().append("Players", this.server.players().stream()
      .map(this::createPlayerInformation)
      .toList());
  }
}
