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
import de.dytanic.cloudnet.common.registry.ServicesRegistry;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceHelper;
import de.dytanic.cloudnet.ext.bridge.platform.PlatformBridgeManagement;
import de.dytanic.cloudnet.ext.bridge.player.NetworkPlayerProxyInfo;
import de.dytanic.cloudnet.ext.bridge.player.PlayerManager;
import de.dytanic.cloudnet.ext.bridge.player.ServicePlayer;
import de.dytanic.cloudnet.ext.bridge.player.executor.PlayerExecutor;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import lombok.NonNull;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.jetbrains.annotations.Nullable;

final class BungeeCordBridgeManagement extends PlatformBridgeManagement<ProxiedPlayer, NetworkPlayerProxyInfo> {

  private static final BiFunction<ProxiedPlayer, String, Boolean> PERM_FUNCTION = CommandSender::hasPermission;

  private final PlayerExecutor globalDirectPlayerExecutor;

  public BungeeCordBridgeManagement() {
    super(Wrapper.instance());
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
      .and(service -> ServiceEnvironmentType.JAVA_SERVER.get(service.serviceId().environment().properties()));
    // register each service matching the service cache tester
    this.cacheRegisterListener = BungeeCordServerHelper.SERVER_REGISTER_HANDLER;
    // unregister each service matching the service cache tester
    this.cacheUnregisterListener = BungeeCordServerHelper.SERVER_UNREGISTER_HANDLER;
  }

  @Override
  public void registerServices(@NonNull ServicesRegistry registry) {
    registry.registerService(PlayerManager.class, "PlayerManager", this.playerManager);
    registry.registerService(PlatformBridgeManagement.class, "BungeeCordBridgeManagement", this);
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
      null,
      player.getPendingConnection().getVersion(),
      new HostAndPort((InetSocketAddress) player.getSocketAddress()),
      new HostAndPort((InetSocketAddress) player.getPendingConnection().getListener().getSocketAddress()),
      player.getPendingConnection().isOnlineMode(),
      this.ownNetworkServiceInfo);
  }

  @Override
  public @NonNull BiFunction<ProxiedPlayer, String, Boolean> permissionFunction() {
    return PERM_FUNCTION;
  }

  @Override
  public boolean isOnAnyFallbackInstance(@NonNull ProxiedPlayer player) {
    return this.isOnAnyFallbackInstance(
      player.getServer() == null ? null : player.getServer().getInfo().getName(),
      this.getVirtualHostString(player.getPendingConnection()),
      player::hasPermission);
  }

  @Override
  public @NonNull Optional<ServiceInfoSnapshot> fallback(@NonNull ProxiedPlayer player) {
    return this.fallback(player, player.getServer() == null ? null : player.getServer().getInfo().getName());
  }

  @Override
  public @NonNull Optional<ServiceInfoSnapshot> fallback(
    @NonNull ProxiedPlayer player,
    @Nullable String currServer
  ) {
    return this.fallback(
      player.getUniqueId(),
      currServer,
      this.getVirtualHostString(player.getPendingConnection()),
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
      : new BungeeCordDirectPlayerExecutor(
        uniqueId,
        this,
        () -> Collections.singleton(ProxyServer.getInstance().getPlayer(uniqueId)));
  }

  @Override
  public void appendServiceInformation(@NonNull ServiceInfoSnapshot snapshot) {
    super.appendServiceInformation(snapshot);
    // append the velocity specific information
    snapshot.properties().append("Online-Count", ProxyServer.getInstance().getPlayers().size());
    snapshot.properties().append("Version", ProxyServer.getInstance().getVersion());
    // players
    snapshot.properties().append("Players", ProxyServer.getInstance().getPlayers().stream()
      .map(this::createPlayerInformation)
      .toList());
  }

  private @Nullable String getVirtualHostString(@NonNull PendingConnection connection) {
    return connection.getVirtualHost() == null ? null : connection.getVirtualHost().getHostString();
  }
}
