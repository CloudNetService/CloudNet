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

package eu.cloudnetservice.modules.bridge.impl.platform.minestom;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.network.rpc.factory.RPCFactory;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.ext.platforminject.api.stereotype.ProvidesFor;
import eu.cloudnetservice.modules.bridge.BridgeManagement;
import eu.cloudnetservice.modules.bridge.BridgeServiceHelper;
import eu.cloudnetservice.modules.bridge.impl.platform.PlatformBridgeManagement;
import eu.cloudnetservice.modules.bridge.impl.util.BridgeHostAndPortUtil;
import eu.cloudnetservice.modules.bridge.player.NetworkPlayerServerInfo;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import eu.cloudnetservice.modules.bridge.player.ServicePlayer;
import eu.cloudnetservice.modules.bridge.player.executor.PlayerExecutor;
import eu.cloudnetservice.wrapper.configuration.WrapperConfiguration;
import eu.cloudnetservice.wrapper.event.ServiceInfoPropertiesConfigureEvent;
import eu.cloudnetservice.wrapper.holder.ServiceInfoHolder;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import lombok.NonNull;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.server.ServerListPingEvent;
import net.minestom.server.network.ConnectionManager;
import net.minestom.server.ping.ServerListPingType;
import org.jetbrains.annotations.Nullable;

@Singleton
@ProvidesFor(platform = "minestom", types = {PlatformBridgeManagement.class, BridgeManagement.class})
public final class MinestomBridgeManagement extends PlatformBridgeManagement<Player, NetworkPlayerServerInfo> {

  private final CommandManager commandManager;
  private final ConnectionManager connectionManager;
  private final PlayerExecutor directGlobalExecutor;
  private final MinestomPermissionChecker permissionChecker;

  @Inject
  public MinestomBridgeManagement(
    @NonNull RPCFactory rpcFactory,
    @NonNull EventManager eventManager,
    @NonNull NetworkClient networkClient,
    @NonNull CommandManager commandManager,
    @NonNull GlobalEventHandler eventHandler,
    @NonNull ServiceTaskProvider taskProvider,
    @NonNull BridgeServiceHelper serviceHelper,
    @NonNull ConnectionManager connectionManager,
    @NonNull ServiceInfoHolder serviceInfoHolder,
    @NonNull CloudServiceProvider serviceProvider,
    @NonNull WrapperConfiguration wrapperConfiguration,
    @NonNull @Service MinestomPermissionChecker permissionChecker
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
    // init fields
    this.commandManager = commandManager;
    this.connectionManager = connectionManager;
    this.permissionChecker = permissionChecker;

    this.directGlobalExecutor = new MinestomDirectPlayerExecutor(
      this.permissionChecker,
      commandManager,
      PlayerExecutor.GLOBAL_UNIQUE_ID,
      connectionManager::getOnlinePlayers);

    // send a ping event to gather the max players and the motd of the server
    var pingEvent = new ServerListPingEvent(ServerListPingType.MODERN_FULL_RGB);
    eventHandler.call(pingEvent);

    // init the bridge properties
    serviceHelper.motd().set(legacySection().serialize(pingEvent.getResponseData().getDescription()));
    serviceHelper.maxPlayers().set(pingEvent.getResponseData().getMaxPlayer());
  }

  @Override
  public void registerServices(@NonNull ServiceRegistry registry) {
    registry.registerProvider(PlayerManager.class, "PlayerManager", this.playerManager);
  }

  @Override
  public @NonNull ServicePlayer wrapPlayer(@NonNull Player player) {
    return new ServicePlayer(player.getUuid(), player.getUsername());
  }

  @Override
  public @NonNull NetworkPlayerServerInfo createPlayerInformation(@NonNull Player player) {
    return new NetworkPlayerServerInfo(
      player.getUuid(),
      player.getUsername(),
      null,
      BridgeHostAndPortUtil.fromSocketAddress(player.getPlayerConnection().getRemoteAddress()),
      this.ownNetworkServiceInfo);
  }

  @Override
  public @NonNull BiFunction<Player, String, Boolean> permissionFunction() {
    return this.permissionChecker::hasPermission;
  }

  @Override
  public boolean isOnAnyFallbackInstance(@NonNull Player player) {
    return this.isOnAnyFallbackInstance(
      this.ownNetworkServiceInfo.serverName(),
      null,
      perm -> this.permissionFunction().apply(player, perm));
  }

  @Override
  public @NonNull Optional<ServiceInfoSnapshot> fallback(@NonNull Player player) {
    return this.fallback(player, this.ownNetworkServiceInfo.serverName());
  }

  @Override
  public @NonNull Optional<ServiceInfoSnapshot> fallback(@NonNull Player player, @Nullable String currServer) {
    return this.fallback(player.getUuid(), currServer, null, perm -> this.permissionFunction().apply(player, perm));
  }

  @Override
  public void handleFallbackConnectionSuccess(@NonNull Player player) {
    this.handleFallbackConnectionSuccess(player.getUuid());
  }

  @Override
  public void removeFallbackProfile(@NonNull Player player) {
    this.removeFallbackProfile(player.getUuid());
  }

  @Override
  public @NonNull PlayerExecutor directPlayerExecutor(@NonNull UUID uniqueId) {
    return uniqueId.equals(PlayerExecutor.GLOBAL_UNIQUE_ID)
      ? this.directGlobalExecutor
      : new MinestomDirectPlayerExecutor(
        this.permissionChecker,
        this.commandManager,
        uniqueId,
        () -> Collections.singleton(this.connectionManager.getOnlinePlayerByUuid(uniqueId)));
  }

  @Override
  public void appendServiceInformation(@NonNull ServiceInfoPropertiesConfigureEvent configureEvent) {
    super.appendServiceInformation(configureEvent);

    // append the minestom specific information
    var onlinePlayers = this.connectionManager.getOnlinePlayers();
    configureEvent.propertyHolder().append("Online-Count", onlinePlayers.size());
    configureEvent.propertyHolder().append("Version", MinecraftServer.VERSION_NAME);
    // players
    configureEvent.propertyHolder().append("Players", onlinePlayers.stream()
      .map(this::createPlayerInformation)
      .toList());
  }
}
