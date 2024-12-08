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

package eu.cloudnetservice.modules.bridge.impl.platform.limboloohp;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

import com.loohp.limbo.Limbo;
import com.loohp.limbo.player.Player;
import com.loohp.limbo.plugins.LimboPlugin;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.network.HostAndPort;
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
import org.jetbrains.annotations.Nullable;

@Singleton
@ProvidesFor(platform = "limboloohp", types = {PlatformBridgeManagement.class, BridgeManagement.class})
final class LimboLoohpBridgeManagement extends PlatformBridgeManagement<Player, NetworkPlayerServerInfo> {

  private static final BiFunction<Player, String, Boolean> PERM_FUNCTION = Player::hasPermission;

  private final Limbo limbo;
  private final LimboPlugin plugin;
  private final PlayerExecutor directGlobalExecutor;

  @Inject
  public LimboLoohpBridgeManagement(
    @NonNull Limbo limbo,
    @NonNull LimboPlugin plugin,
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
    // init fields
    this.limbo = limbo;
    this.plugin = plugin;
    this.directGlobalExecutor = new LimboLoohpDirectPlayerExecutor(
      plugin,
      PlayerExecutor.GLOBAL_UNIQUE_ID,
      limbo::getPlayers);
    // init the bridge properties
    serviceHelper.motd().set(legacySection().serialize(limbo.getServerProperties().getMotd()));
    serviceHelper.maxPlayers().set(limbo.getServerProperties().getMaxPlayers());
  }

  @Override
  public void registerServices(@NonNull ServiceRegistry registry) {
    registry.registerProvider(PlayerManager.class, "PlayerManager", this.playerManager);
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
      new HostAndPort(
        player.clientConnection.getInetAddress().getHostAddress(),
        player.clientConnection.getSocket().getPort()
      ),
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
      : new LimboLoohpDirectPlayerExecutor(
        this.plugin,
        uniqueId,
        () -> Collections.singleton(this.limbo.getPlayer(uniqueId)));
  }

  @Override
  public void appendServiceInformation(@NonNull ServiceInfoPropertiesConfigureEvent configureEvent) {
    super.appendServiceInformation(configureEvent);

    // append the bukkit specific information
    configureEvent.propertyHolder().append("Version", this.limbo.getServerProperties().getVersionString());
    configureEvent.propertyHolder().append("Online-Count", this.limbo.getPlayers().size());
    // players
    configureEvent.propertyHolder().append("Players", this.limbo.getPlayers().stream()
      .map(this::createPlayerInformation)
      .toList());
  }
}
