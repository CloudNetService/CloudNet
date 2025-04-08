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

package eu.cloudnetservice.modules.bridge.impl.platform.velocity;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

import com.velocitypowered.api.permission.PermissionSubject;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
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
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@Singleton
@ProvidesFor(platform = "velocity", types = {PlatformBridgeManagement.class, BridgeManagement.class})
final class VelocityBridgeManagement extends PlatformBridgeManagement<Player, NetworkPlayerProxyInfo> {

  private static final BiFunction<Player, String, Boolean> PERM_FUNCTION = PermissionSubject::hasPermission;

  private final ProxyServer proxyServer;
  private final PlayerExecutor globalDirectPlayerExecutor;

  @Inject
  public VelocityBridgeManagement(
    @NonNull RPCFactory rpcFactory,
    @NonNull ProxyServer proxyServer,
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
    this.proxyServer = proxyServer;
    this.globalDirectPlayerExecutor = new VelocityDirectPlayerExecutor(
      PlayerExecutor.GLOBAL_UNIQUE_ID,
      proxyServer,
      this,
      proxyServer::getAllPlayers);
    // init the bridge properties
    serviceHelper.motd().set(legacySection().serialize(proxyServer.getConfiguration().getMotd()));
    serviceHelper.maxPlayers().set(proxyServer.getConfiguration().getShowMaxPlayers());
    // init the default cache listeners
    this.cacheTester = CONNECTED_SERVICE_TESTER
      .and(service -> service.serviceId().environment().readProperty(ServiceEnvironmentType.JAVA_SERVER));
    // register each service matching the service cache tester
    this.cacheRegisterListener = service -> proxyServer.registerServer(new ServerInfo(
      service.name(),
      new InetSocketAddress(service.address().host(), service.address().port())));
    // unregister each service matching the service cache tester
    this.cacheUnregisterListener = service -> proxyServer.getServer(service.name())
      .map(RegisteredServer::getServerInfo)
      .ifPresent(proxyServer::unregisterServer);
  }

  @Override
  public void registerServices(@NonNull ServiceRegistry registry) {
    registry.registerProvider(PlayerManager.class, "PlayerManager", this.playerManager);
  }

  @Override
  public @NonNull ServicePlayer wrapPlayer(@NonNull Player player) {
    return new ServicePlayer(player.getUniqueId(), player.getUsername());
  }

  @Override
  public @NonNull NetworkPlayerProxyInfo createPlayerInformation(@NonNull Player player) {
    return new NetworkPlayerProxyInfo(
      player.getUniqueId(),
      player.getUsername(),
      null,
      player.getProtocolVersion().getProtocol(),
      BridgeHostAndPortUtil.fromSocketAddress(player.getRemoteAddress()),
      BridgeHostAndPortUtil.fromSocketAddress(this.proxyServer.getBoundAddress()),
      player.isOnlineMode(),
      this.ownNetworkServiceInfo);
  }

  @Override
  public @NonNull BiFunction<Player, String, Boolean> permissionFunction() {
    return PERM_FUNCTION;
  }

  @Override
  public boolean isOnAnyFallbackInstance(@NonNull Player player) {
    return this.isOnAnyFallbackInstance(
      player.getCurrentServer().map(connection -> connection.getServerInfo().getName()).orElse(null),
      player.getVirtualHost().map(InetSocketAddress::getHostString).orElse(null),
      player::hasPermission);
  }

  @Override
  public @NonNull Optional<ServiceInfoSnapshot> fallback(@NonNull Player player) {
    return this.fallback(
      player,
      player.getCurrentServer().map(connection -> connection.getServerInfo().getName()).orElse(null));
  }

  @Override
  public @NonNull Optional<ServiceInfoSnapshot> fallback(@NonNull Player player, @Nullable String currServer) {
    return this.fallback(
      player.getUniqueId(),
      currServer,
      player.getVirtualHost().map(InetSocketAddress::getHostString).orElse(null),
      player::hasPermission);
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
      ? this.globalDirectPlayerExecutor
      : new VelocityDirectPlayerExecutor(
        uniqueId,
        this.proxyServer,
        this,
        () -> Collections.singleton(this.proxyServer.getPlayer(uniqueId).orElse(null)));
  }

  @Override
  public void appendServiceInformation(@NonNull ServiceInfoPropertiesConfigureEvent configureEvent) {
    super.appendServiceInformation(configureEvent);

    // append the velocity specific information
    configureEvent.propertyHolder().append("Online-Count", this.proxyServer.getPlayerCount());
    configureEvent.propertyHolder().append("Version", this.proxyServer.getVersion().getVersion());
    // players
    configureEvent.propertyHolder().append("Players", this.proxyServer.getAllPlayers().stream()
      .map(this::createPlayerInformation)
      .toList());
  }
}
