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

package de.dytanic.cloudnet.ext.bridge.platform.bungeecord;

import com.google.common.collect.Iterables;
import de.dytanic.cloudnet.common.registry.IServicesRegistry;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
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
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class BungeeCordBridgeManagement extends PlatformBridgeManagement<ProxiedPlayer, NetworkPlayerProxyInfo> {

  private static final BiFunction<ProxiedPlayer, String, Boolean> PERM_FUNCTION = CommandSender::hasPermission;

  private final PlayerExecutor globalDirectPlayerExecutor;

  public BungeeCordBridgeManagement() {
    super(Wrapper.getInstance());
    // init fields
    this.globalDirectPlayerExecutor = new BungeeCordDirectPlayerExecutor(
      PlayerExecutor.GLOBAL_UNIQUE_ID,
      this,
      ProxyServer.getInstance()::getPlayers);
    // init the bridge properties
    BridgeServiceHelper.MOTD.set(Iterables.get(ProxyServer.getInstance().getConfig().getListeners(), 0).getMotd());
    BridgeServiceHelper.MAX_PLAYERS.set(ProxyServer.getInstance().getConfig().getPlayerLimit());
    // init the default cache listeners
    this.cacheTester = CONNECTED_SERVICE_TESTER
      .and(service -> ServiceEnvironmentType.JAVA_SERVER.get(service.getServiceId().getEnvironment().properties()));
    // register each service matching the service cache tester
    this.cacheRegisterListener = BungeeCordServerHelper.SERVER_REGISTER_HANDLER;
    // unregister each service matching the service cache tester
    this.cacheUnregisterListener = BungeeCordServerHelper.SERVER_UNREGISTER_HANDLER;
  }

  @Override
  public void registerServices(@NotNull IServicesRegistry registry) {
    registry.registerService(IPlayerManager.class, "PlayerManager", this.playerManager);
    registry.registerService(PlatformBridgeManagement.class, "BungeeCordBridgeManagement", this);
  }

  @Override
  public @NotNull ServicePlayer wrapPlayer(@NotNull ProxiedPlayer player) {
    return new ServicePlayer(player.getUniqueId(), player.getName());
  }

  @Override
  public @NotNull NetworkPlayerProxyInfo createPlayerInformation(@NotNull ProxiedPlayer player) {
    return new NetworkPlayerProxyInfo(
      player.getUniqueId(),
      player.getName(),
      null,
      player.getPendingConnection().getVersion(),
      new HostAndPort((InetSocketAddress) player.getSocketAddress()),
      new HostAndPort((InetSocketAddress) player.getPendingConnection().getListener().getSocketAddress()),
      player.getPendingConnection().isOnlineMode(),
      this.ownNetworkServiceInfo);
  }

  @Override
  public @NotNull BiFunction<ProxiedPlayer, String, Boolean> getPermissionFunction() {
    return PERM_FUNCTION;
  }

  @Override
  public boolean isOnAnyFallbackInstance(@NotNull ProxiedPlayer player) {
    return this.isOnAnyFallbackInstance(
      player.getServer() == null ? null : player.getServer().getInfo().getName(),
      this.getVirtualHostString(player.getPendingConnection()),
      player::hasPermission);
  }

  @Override
  public @NotNull Optional<ServiceInfoSnapshot> getFallback(@NotNull ProxiedPlayer player) {
    return this.getFallback(player, player.getServer() == null ? null : player.getServer().getInfo().getName());
  }

  @Override
  public @NotNull Optional<ServiceInfoSnapshot> getFallback(
    @NotNull ProxiedPlayer player,
    @Nullable String currServer
  ) {
    return this.getFallback(
      player.getUniqueId(),
      currServer,
      this.getVirtualHostString(player.getPendingConnection()),
      player::hasPermission);
  }

  @Override
  public void handleFallbackConnectionSuccess(@NotNull ProxiedPlayer player) {
    this.handleFallbackConnectionSuccess(player.getUniqueId());
  }

  @Override
  public void removeFallbackProfile(@NotNull ProxiedPlayer player) {
    this.removeFallbackProfile(player.getUniqueId());
  }

  @Override
  public @NotNull PlayerExecutor getDirectPlayerExecutor(@NotNull UUID uniqueId) {
    return uniqueId.equals(PlayerExecutor.GLOBAL_UNIQUE_ID)
      ? this.globalDirectPlayerExecutor
      : new BungeeCordDirectPlayerExecutor(
        uniqueId,
        this,
        () -> Collections.singleton(ProxyServer.getInstance().getPlayer(uniqueId)));
  }

  @Override
  public void appendServiceInformation(@NotNull ServiceInfoSnapshot snapshot) {
    super.appendServiceInformation(snapshot);
    // append the velocity specific information
    snapshot.properties().append("Online-Count", ProxyServer.getInstance().getPlayers().size());
    snapshot.properties().append("Version", ProxyServer.getInstance().getVersion());
    // players
    snapshot.properties().append("Players", ProxyServer.getInstance().getPlayers().stream()
      .map(this::createPlayerInformation)
      .collect(Collectors.toList()));
  }

  private @Nullable String getVirtualHostString(@NotNull PendingConnection connection) {
    return connection.getVirtualHost() == null ? null : connection.getVirtualHost().getHostString();
  }
}
