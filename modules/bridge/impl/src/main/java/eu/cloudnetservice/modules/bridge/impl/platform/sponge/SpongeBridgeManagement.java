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

package eu.cloudnetservice.modules.bridge.impl.platform.sponge;

import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.network.rpc.factory.RPCFactory;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
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
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Server;
import org.spongepowered.api.command.manager.CommandManager;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.network.channel.ChannelManager;
import org.spongepowered.api.service.permission.Subject;

@Singleton
@ProvidesFor(platform = "sponge", types = {PlatformBridgeManagement.class, BridgeManagement.class})
final class SpongeBridgeManagement extends PlatformBridgeManagement<ServerPlayer, NetworkPlayerServerInfo> {

  private static final BiFunction<ServerPlayer, String, Boolean> PERM_FUNCTION = Subject::hasPermission;

  private final Server server;
  private final Platform platform;
  private final CommandManager commandManager;
  private final ChannelManager channelManager;
  private final PlayerExecutor directGlobalExecutor;
  private final SpongeAddressAccessor spongeAddressAccessor;

  @Inject
  public SpongeBridgeManagement(
    @NonNull Server server,
    @NonNull Platform platform,
    @NonNull RPCFactory rpcFactory,
    @NonNull EventManager eventManager,
    @NonNull NetworkClient networkClient,
    @NonNull ChannelManager channelManager,
    @NonNull CommandManager commandManager,
    @NonNull ServiceTaskProvider taskProvider,
    @NonNull BridgeServiceHelper serviceHelper,
    @NonNull ServiceInfoHolder serviceInfoHolder,
    @NonNull CloudServiceProvider serviceProvider,
    @NonNull WrapperConfiguration wrapperConfiguration,
    @NonNull SpongeAddressAccessor spongeAddressAccessor
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
    this.server = server;
    this.platform = platform;
    this.commandManager = commandManager;
    this.channelManager = channelManager;
    this.spongeAddressAccessor = spongeAddressAccessor;
    this.directGlobalExecutor = new SpongeDirectPlayerExecutor(
      channelManager,
      commandManager,
      PlayerExecutor.GLOBAL_UNIQUE_ID,
      this.server::onlinePlayers);
    // init the bridge properties
    serviceHelper.maxPlayers().set(this.server.maxPlayers());
    serviceHelper.motd().set(LegacyComponentSerializer.legacySection().serialize(this.server.motd()));
  }

  @Override
  public void registerServices(@NonNull ServiceRegistry registry) {
    registry.registerProvider(PlayerManager.class, "PlayerManager", this.playerManager);
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
      BridgeHostAndPortUtil.fromSocketAddress(this.spongeAddressAccessor.playerHostAddress(player)),
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
        this.channelManager,
        this.commandManager,
        uniqueId,
        () -> Collections.singleton(this.server.player(uniqueId).orElse(null)));
  }

  @Override
  public void appendServiceInformation(@NonNull ServiceInfoPropertiesConfigureEvent configureEvent) {
    super.appendServiceInformation(configureEvent);

    // append the bukkit specific information
    configureEvent.propertyHolder().append("Online-Count", this.server.onlinePlayers().size());
    configureEvent.propertyHolder().append("Version", this.platform.minecraftVersion().name());
    // players
    configureEvent.propertyHolder().append("Players", this.server.onlinePlayers().stream()
      .map(this::createPlayerInformation)
      .toList());
  }
}
