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

package eu.cloudnetservice.modules.bridge.platform.waterdog;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.WaterdogPE;
import dev.waterdog.waterdogpe.command.CommandSender;
import dev.waterdog.waterdogpe.network.serverinfo.BedrockServerInfo;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import eu.cloudnetservice.cloudnet.common.registry.ServicesRegistry;
import eu.cloudnetservice.cloudnet.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.cloudnet.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.cloudnet.wrapper.Wrapper;
import eu.cloudnetservice.modules.bridge.BridgeServiceHelper;
import eu.cloudnetservice.modules.bridge.platform.PlatformBridgeManagement;
import eu.cloudnetservice.modules.bridge.player.NetworkPlayerProxyInfo;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import eu.cloudnetservice.modules.bridge.player.ServicePlayer;
import eu.cloudnetservice.modules.bridge.player.executor.PlayerExecutor;
import eu.cloudnetservice.modules.bridge.util.BridgeHostAndPortUtil;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

final class WaterDogPEBridgeManagement extends PlatformBridgeManagement<ProxiedPlayer, NetworkPlayerProxyInfo> {

  private static final BiFunction<ProxiedPlayer, String, Boolean> PERM_FUNCTION = CommandSender::hasPermission;

  private final PlayerExecutor globalDirectPlayerExecutor;

  public WaterDogPEBridgeManagement() {
    super(Wrapper.instance());
    // init fields
    this.globalDirectPlayerExecutor = new WaterDogPEDirectPlayerExecutor(
      PlayerExecutor.GLOBAL_UNIQUE_ID,
      this,
      ProxyServer.getInstance().getPlayers()::values);
    // init the bridge properties
    BridgeServiceHelper.MOTD.set(ProxyServer.getInstance().getConfiguration().getMotd());
    BridgeServiceHelper.MAX_PLAYERS.set(ProxyServer.getInstance().getConfiguration().getMaxPlayerCount());
    // init the default cache listeners
    this.cacheTester = CONNECTED_SERVICE_TESTER
      .and(service -> ServiceEnvironmentType.PE_SERVER.get(service.serviceId().environment().properties()));
    // register each service matching the service cache tester
    this.cacheRegisterListener = service -> ProxyServer.getInstance().getServerInfoMap().put(
      service.name(),
      new BedrockServerInfo(
        service.name(),
        new InetSocketAddress(service.connectAddress().host(), service.connectAddress().port()),
        new InetSocketAddress(service.connectAddress().host(), service.connectAddress().port())));
    // unregister each service matching the service cache tester
    this.cacheUnregisterListener = service -> ProxyServer.getInstance().getServerInfoMap().remove(service.name());
  }

  @Override
  public void registerServices(@NonNull ServicesRegistry registry) {
    registry.registerService(PlayerManager.class, "PlayerManager", this.playerManager);
    registry.registerService(PlatformBridgeManagement.class, "WaterDogPEBridgeManagement", this);
  }

  @Override
  public @NonNull ServicePlayer wrapPlayer(@NonNull ProxiedPlayer player) {
    return new ServicePlayer(player.getUniqueId(), player.getName());
  }

  @Override
  public @NonNull NetworkPlayerProxyInfo createPlayerInformation(@NonNull ProxiedPlayer player) {
    return new NetworkPlayerProxyInfo(
      player.getUniqueId(),
      player.getName(),
      player.getXuid(),
      player.getProtocol().getRaknetVersion(),
      BridgeHostAndPortUtil.fromSocketAddress(player.getAddress()),
      BridgeHostAndPortUtil.fromSocketAddress(ProxyServer.getInstance().getConfiguration().getBindAddress()),
      player.getLoginData().isXboxAuthed(),
      this.ownNetworkServiceInfo);
  }

  @Override
  public @NonNull BiFunction<ProxiedPlayer, String, Boolean> permissionFunction() {
    return PERM_FUNCTION;
  }

  @Override
  public boolean isOnAnyFallbackInstance(@NonNull ProxiedPlayer player) {
    return this.isOnAnyFallbackInstance(
      player.getServerInfo() == null ? null : player.getServerInfo().getServerName(),
      player.getLoginData().getJoinHostname(),
      player::hasPermission);
  }

  @Override
  public @NonNull Optional<ServiceInfoSnapshot> fallback(@NonNull ProxiedPlayer player) {
    return this.fallback(player, player.getServerInfo() == null ? null : player.getServerInfo().getServerName());
  }

  @Override
  public @NonNull Optional<ServiceInfoSnapshot> fallback(
    @NonNull ProxiedPlayer player,
    @Nullable String currServer
  ) {
    return this.fallback(
      player.getUniqueId(),
      currServer,
      player.getLoginData().getJoinHostname(),
      player::hasPermission);
  }

  @Override
  public void handleFallbackConnectionSuccess(@NonNull ProxiedPlayer player) {
    this.handleFallbackConnectionSuccess(player.getUniqueId());
  }

  @Override
  public void removeFallbackProfile(@NonNull ProxiedPlayer player) {
    this.removeFallbackProfile(player.getUniqueId());
  }

  @Override
  public @NonNull PlayerExecutor directPlayerExecutor(@NonNull UUID uniqueId) {
    return uniqueId.equals(PlayerExecutor.GLOBAL_UNIQUE_ID)
      ? this.globalDirectPlayerExecutor
      : new WaterDogPEDirectPlayerExecutor(
        uniqueId,
        this,
        () -> Collections.singleton(ProxyServer.getInstance().getPlayer(uniqueId)));
  }

  @Override
  public void appendServiceInformation(@NonNull ServiceInfoSnapshot snapshot) {
    super.appendServiceInformation(snapshot);
    // append the velocity specific information
    snapshot.properties().append("Online-Count", ProxyServer.getInstance().getPlayers().size());
    snapshot.properties().append("Version", WaterdogPE.version().baseVersion());
    // players
    snapshot.properties().append("Players", ProxyServer.getInstance().getPlayers().values().stream()
      .map(this::createPlayerInformation)
      .toList());
  }
}
