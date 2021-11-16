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

package de.dytanic.cloudnet.ext.bridge.platform.velocity;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

import com.velocitypowered.api.permission.PermissionSubject;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import de.dytanic.cloudnet.common.registry.IServicesRegistry;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceHelper;
import de.dytanic.cloudnet.ext.bridge.platform.PlatformBridgeManagement;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import de.dytanic.cloudnet.ext.bridge.player.NetworkPlayerProxyInfo;
import de.dytanic.cloudnet.ext.bridge.player.ServicePlayer;
import de.dytanic.cloudnet.ext.bridge.player.executor.PlayerExecutor;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

final class VelocityBridgeManagement extends PlatformBridgeManagement<Player, NetworkPlayerProxyInfo> {

  private static final BiFunction<Player, String, Boolean> PERM_FUNCTION = PermissionSubject::hasPermission;

  private final ProxyServer proxyServer;
  private final PlayerExecutor globalDirectPlayerExecutor;

  public VelocityBridgeManagement(@NotNull ProxyServer proxyServer) {
    super(Wrapper.getInstance());
    // init fields
    this.proxyServer = proxyServer;
    this.globalDirectPlayerExecutor = new VelocityDirectPlayerExecutor(
      PlayerExecutor.GLOBAL_UNIQUE_ID,
      proxyServer,
      this,
      proxyServer::getAllPlayers);
    // init the bridge properties
    BridgeServiceHelper.MOTD.set(legacySection().serialize(proxyServer.getConfiguration().getMotd()));
    BridgeServiceHelper.MAX_PLAYERS.set(proxyServer.getConfiguration().getShowMaxPlayers());
    // init the default cache listeners
    this.cacheTester = CONNECTED_SERVICE_TESTER
      .and(service -> service.getServiceId().getEnvironment().isMinecraftJavaServer());
    // register each service matching the service cache tester
    this.cacheRegisterListener = service -> proxyServer.registerServer(new ServerInfo(
      service.getName(),
      new InetSocketAddress(service.getConnectAddress().getHost(), service.getConnectAddress().getPort())));
    // unregister each service matching the service cache tester
    this.cacheUnregisterListener = service -> proxyServer.getServer(service.getName())
      .map(RegisteredServer::getServerInfo)
      .ifPresent(proxyServer::unregisterServer);
  }

  @Override
  public void registerServices(@NotNull IServicesRegistry registry) {
    registry.registerService(IPlayerManager.class, "PlayerManager", this.playerManager);
    registry.registerService(PlatformBridgeManagement.class, "VelocityBridgeManagement", this);
  }

  @Override
  public @NotNull ServicePlayer wrapPlayer(@NotNull Player player) {
    return new ServicePlayer(player.getUniqueId(), player.getUsername());
  }

  @Override
  public @NotNull NetworkPlayerProxyInfo createPlayerInformation(@NotNull Player player) {
    return new NetworkPlayerProxyInfo(
      player.getUniqueId(),
      player.getUsername(),
      null,
      player.getProtocolVersion().getProtocol(),
      new HostAndPort(player.getRemoteAddress()),
      new HostAndPort(this.proxyServer.getBoundAddress()),
      player.isOnlineMode(),
      this.ownNetworkServiceInfo);
  }

  @Override
  public @NotNull BiFunction<Player, String, Boolean> getPermissionFunction() {
    return PERM_FUNCTION;
  }

  @Override
  public boolean isOnAnyFallbackInstance(@NotNull Player player) {
    return this.isOnAnyFallbackInstance(
      player.getCurrentServer().map(connection -> connection.getServerInfo().getName()).orElse(null),
      player.getVirtualHost().map(InetSocketAddress::getHostString).orElse(null),
      player::hasPermission);
  }

  @Override
  public @NotNull Optional<ServiceInfoSnapshot> getFallback(@NotNull Player player) {
    return this.getFallback(
      player.getUniqueId(),
      player.getCurrentServer().map(connection -> connection.getServerInfo().getName()).orElse(null),
      player.getVirtualHost().map(InetSocketAddress::getHostString).orElse(null),
      player::hasPermission);
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
      ? this.globalDirectPlayerExecutor
      : new VelocityDirectPlayerExecutor(
        uniqueId,
        this.proxyServer,
        this,
        () -> Collections.singleton(this.proxyServer.getPlayer(uniqueId).orElse(null)));
  }

  @Override
  public void appendServiceInformation(@NotNull ServiceInfoSnapshot snapshot) {
    super.appendServiceInformation(snapshot);
    // append the velocity specific information
    snapshot.getProperties().append("Online-Count", this.proxyServer.getPlayerCount());
    snapshot.getProperties().append("Version", this.proxyServer.getVersion().getVersion());
    // players
    snapshot.getProperties().append("Players", this.proxyServer.getAllPlayers().stream()
      .map(this::createPlayerInformation)
      .collect(Collectors.toList()));
  }
}
