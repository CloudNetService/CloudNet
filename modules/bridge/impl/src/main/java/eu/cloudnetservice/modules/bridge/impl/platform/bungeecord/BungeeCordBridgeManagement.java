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

package eu.cloudnetservice.modules.bridge.impl.platform.bungeecord;

import com.google.common.collect.Iterables;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.network.rpc.factory.RPCFactory;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.ext.platforminject.api.stereotype.ProvidesFor;
import eu.cloudnetservice.modules.bridge.BridgeManagement;
import eu.cloudnetservice.modules.bridge.BridgeServiceHelper;
import eu.cloudnetservice.modules.bridge.impl.platform.PlatformBridgeManagement;
import eu.cloudnetservice.modules.bridge.impl.util.BridgeHostAndPortUtil;
import eu.cloudnetservice.modules.bridge.player.NetworkPlayerProxyInfo;
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
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.jetbrains.annotations.Nullable;

@Singleton
@ProvidesFor(platform = "bungeecord", types = {PlatformBridgeManagement.class, BridgeManagement.class})
final class BungeeCordBridgeManagement extends PlatformBridgeManagement<ProxiedPlayer, NetworkPlayerProxyInfo> {

  private static final BiFunction<ProxiedPlayer, String, Boolean> PERM_FUNCTION = CommandSender::hasPermission;

  private final ProxyServer proxyServer;
  private final PlayerExecutor globalDirectPlayerExecutor;

  @Inject
  public BungeeCordBridgeManagement(
    @NonNull RPCFactory rpcFactory,
    @NonNull ProxyServer proxyServer,
    @NonNull EventManager eventManager,
    @NonNull NetworkClient networkClient,
    @NonNull BungeeCordHelper bungeeHelper,
    @NonNull ServiceTaskProvider taskProvider,
    @NonNull BridgeServiceHelper serviceHelper,
    @NonNull ServiceInfoHolder serviceInfoHolder,
    @NonNull CloudServiceProvider serviceProvider,
    @NonNull WrapperConfiguration wrapperConfiguration
  ) {
    super(rpcFactory,
      eventManager,
      networkClient,
      taskProvider,
      serviceHelper,
      serviceInfoHolder,
      serviceProvider,
      wrapperConfiguration);
    // init fields
    this.proxyServer = proxyServer;
    this.globalDirectPlayerExecutor = new BungeeCordDirectPlayerExecutor(
      proxyServer,
      PlayerExecutor.GLOBAL_UNIQUE_ID,
      this,
      proxyServer::getPlayers);
    // init the bridge properties
    serviceHelper.motd().set(Iterables.get(this.proxyServer.getConfig().getListeners(), 0).getMotd());
    serviceHelper.maxPlayers().set(this.proxyServer.getConfig().getPlayerLimit());
    // init the default cache listeners
    this.cacheTester = CONNECTED_SERVICE_TESTER
      .and(service -> service.serviceId().environment().readProperty(ServiceEnvironmentType.JAVA_SERVER));
    // register each service matching the service cache tester
    this.cacheRegisterListener = bungeeHelper.serverRegisterHandler();
    // unregister each service matching the service cache tester
    this.cacheUnregisterListener = bungeeHelper.serverUnregisterHandler();
  }

  @Override
  public void registerServices(@NonNull ServiceRegistry registry) {
    registry.registerProvider(PlayerManager.class, "PlayerManager", this.playerManager);
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
      BridgeHostAndPortUtil.fromSocketAddress(player.getSocketAddress()),
      BridgeHostAndPortUtil.fromSocketAddress(player.getPendingConnection().getListener().getSocketAddress()),
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
        this.proxyServer,
        uniqueId,
        this,
        () -> Collections.singleton(this.proxyServer.getPlayer(uniqueId)));
  }

  @Override
  public void appendServiceInformation(@NonNull ServiceInfoPropertiesConfigureEvent configureEvent) {
    super.appendServiceInformation(configureEvent);

    // append the velocity specific information
    configureEvent.propertyHolder().append("Version", this.proxyServer.getVersion());
    configureEvent.propertyHolder().append("Online-Count", this.proxyServer.getPlayers().size());
    // players
    configureEvent.propertyHolder().append("Players", this.proxyServer.getPlayers().stream()
      .map(this::createPlayerInformation)
      .toList());
  }

  private @Nullable String getVirtualHostString(@NonNull PendingConnection connection) {
    return connection.getVirtualHost() == null ? null : connection.getVirtualHost().getHostString();
  }
}
