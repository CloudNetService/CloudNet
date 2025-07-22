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

import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import io.vavr.CheckedConsumer;
import io.vavr.CheckedPredicate;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.InetSocketAddress;
import lombok.NonNull;
import net.md_5.bungee.api.ProxyConfig;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.protocol.ProtocolConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper utilities for bungeecord-related functionalities.
 *
 * @since 4.0
 */
@Singleton
final class BungeeCordHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(BungeeCordHelper.class);

  // tests if a player was connected to the proxy before. the last possible usage point is ServerConnectedEvent
  private static final CheckedPredicate<ProxiedPlayer> PLAYER_INITIAL_CONNECT_TESTER;

  static {
    var lookup = MethodHandles.publicLookup(); // everything should be public api

    CheckedPredicate<ProxiedPlayer> playerInitialConnectTester;
    try {
      // modern access: via UserConnection.getDimension():Object (converted to ProxiedPlayer.getDimension():Object)
      var userConnectionClass = Class.forName("net.md_5.bungee.UserConnection");
      var getDimension = lookup.findVirtual(userConnectionClass, "getDimension", MethodType.methodType(Object.class));
      var getDimensionProxiesPlayer = MethodHandles.explicitCastArguments(
        getDimension,
        MethodType.methodType(Object.class, ProxiedPlayer.class));
      playerInitialConnectTester = proxiedPlayer -> {
        // login packet sequence changed in 1.20.2, before that checking via getServer() == null is fine
        var playerVersion = proxiedPlayer.getPendingConnection().getVersion();
        if (playerVersion >= ProtocolConstants.MINECRAFT_1_20_2) {
          var dimension = (Object) getDimensionProxiesPlayer.invokeExact(proxiedPlayer);
          return dimension == null;
        } else {
          return proxiedPlayer.getServer() == null;
        }
      };
      LOGGER.debug("net.md_5.bungee.UserConnection.getDimension(): available");
    } catch (Exception ex) {
      // fallback to check via ProxiedPlayer.getServer()
      playerInitialConnectTester = proxiedPlayer -> proxiedPlayer.getServer() == null;
      LOGGER.debug("net.md_5.bungee.UserConnection.getDimension(): unavailable ({})", ex.getMessage());
    }

    PLAYER_INITIAL_CONNECT_TESTER = playerInitialConnectTester;
  }

  private final ProxyServer proxyServer;
  private final CheckedConsumer<ServiceInfoSnapshot> serverRegisterHandler;
  private final CheckedConsumer<ServiceInfoSnapshot> serverUnregisterHandler;

  @Inject
  public BungeeCordHelper(
    @NonNull ProxyServer proxyServer,
    @NonNull ProxyConfig proxyConfig
  ) {
    this.proxyServer = proxyServer;

    var lookup = MethodHandles.publicLookup(); // everything should be public api

    CheckedConsumer<ServiceInfoSnapshot> serverRegisterHandler;
    try {
      // waterfall: ProxyConfig.addServer(ServerInfo):ServerInfo
      var addServer = lookup.findVirtual(
        ProxyConfig.class,
        "addServer",
        MethodType.methodType(ServerInfo.class, ServerInfo.class));
      var addServerVoid = MethodHandles.dropReturn(addServer);
      var addServerBound = addServerVoid.bindTo(proxyConfig);
      serverRegisterHandler = serviceInfoSnapshot -> {
        var serverInfo = this.constructServerInfo(serviceInfoSnapshot);
        addServerBound.invokeExact(serverInfo);
      };
      LOGGER.debug("net.md_5.bungee.api.ProxyConfig.addServer(ServerInfo): available");
    } catch (NoSuchMethodException | IllegalAccessException ex) {
      // bungeecord: ProxyServer.getServers().put(String, ServerInfo)
      serverRegisterHandler = serviceInfoSnapshot -> {
        var serverInfo = this.constructServerInfo(serviceInfoSnapshot);
        proxyServer.getServers().put(serverInfo.getName(), serverInfo);
      };
      LOGGER.debug("net.md_5.bungee.api.ProxyConfig.addServer(ServerInfo): unavailable ({})", ex.getMessage());
    }

    CheckedConsumer<ServiceInfoSnapshot> serverUnregisterHandler;
    try {
      // waterfall: ProxyConfig.removeServerNamed(String):ServerInfo
      var removeServerNamed = lookup.findVirtual(
        ProxyConfig.class,
        "removeServerNamed",
        MethodType.methodType(ServerInfo.class, String.class));
      var removeServerNamedVoid = MethodHandles.dropReturn(removeServerNamed);
      var removeServerBound = removeServerNamedVoid.bindTo(proxyConfig);
      serverUnregisterHandler = serviceInfoSnapshot -> {
        var serverName = serviceInfoSnapshot.serviceId().name();
        removeServerBound.invokeExact(serverName);
      };
      LOGGER.debug("net.md_5.bungee.api.ProxyConfig.removeServerNamed(String): available");
    } catch (NoSuchMethodException | IllegalAccessException ex) {
      // bungeecord: ProxyServer.getServers().remove(String)
      serverUnregisterHandler = serviceInfoSnapshot -> {
        var serverName = serviceInfoSnapshot.serviceId().name();
        proxyServer.getServers().remove(serverName);
      };
      LOGGER.debug("net.md_5.bungee.api.ProxyConfig.removeServerNamed(String): unavailable ({})", ex.getMessage());
    }

    this.serverRegisterHandler = serverRegisterHandler;
    this.serverUnregisterHandler = serverUnregisterHandler;
  }

  /**
   * Tests if the connection of the given player is initial, returning {@code false} if the player did successfully
   * connect to a backend service before. The last point in the login sequence where this check returns a valid result
   * is in {@code ServerConnectedEvent}.
   *
   * @param player the player to test.
   * @return true if the player was not connected to any service before, false otherwise.
   * @throws NullPointerException  if the given player is null.
   * @throws IllegalStateException if the test failed for some reason.
   */
  public static boolean isInitialConnect(@NonNull ProxiedPlayer player) {
    try {
      return PLAYER_INITIAL_CONNECT_TESTER.test(player);
    } catch (Throwable throwable) {
      throw new IllegalStateException("Unable to determine if player was connected before", throwable);
    }
  }

  /**
   * Register the given service to the proxy. An existing service with the same name will be replaced.
   *
   * @param serviceInfoSnapshot the snapshot of the service to register.
   * @throws NullPointerException if the given service info snapshot is null.
   */
  public void registerServer(@NonNull ServiceInfoSnapshot serviceInfoSnapshot) {
    try {
      this.serverRegisterHandler.accept(serviceInfoSnapshot);
    } catch (Throwable throwable) {
      LOGGER.error("Could not register server {}", serviceInfoSnapshot.serviceId(), throwable);
    }
  }

  /**
   * Unregisters the given service from the proxy.
   *
   * @param serviceInfoSnapshot the snapshot of the service to unregister.
   * @throws NullPointerException if the given service info snapshot is null.
   */
  public void unregisterServer(@NonNull ServiceInfoSnapshot serviceInfoSnapshot) {
    try {
      this.serverUnregisterHandler.accept(serviceInfoSnapshot);
    } catch (Throwable throwable) {
      LOGGER.error("Could not unregister server {}", serviceInfoSnapshot.serviceId(), throwable);
    }
  }

  /**
   * Constructs a proxy server info from the given service info snapshot.
   *
   * @param serviceInfo the service info to construct a server info from.
   * @return a server info constructed from the given service info snapshot.
   * @throws NullPointerException if the given service info snapshot is null.
   */
  private @NonNull ServerInfo constructServerInfo(@NonNull ServiceInfoSnapshot serviceInfo) {
    var serviceAddress = new InetSocketAddress(serviceInfo.address().host(), serviceInfo.address().port());
    return this.proxyServer.constructServerInfo(serviceInfo.name(), serviceAddress, "", false);
  }
}
