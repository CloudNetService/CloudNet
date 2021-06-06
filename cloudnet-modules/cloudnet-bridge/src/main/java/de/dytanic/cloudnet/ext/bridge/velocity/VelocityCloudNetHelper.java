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

package de.dytanic.cloudnet.ext.bridge.velocity;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.PluginInfo;
import de.dytanic.cloudnet.ext.bridge.ProxyFallbackConfiguration;
import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import de.dytanic.cloudnet.ext.bridge.proxy.BridgeProxyHelper;
import de.dytanic.cloudnet.ext.bridge.velocity.event.VelocityPlayerFallbackEvent;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class VelocityCloudNetHelper {

  /**
   * @deprecated use {@link BridgeProxyHelper#getCachedServiceInfoSnapshot(String)} or {@link
   * BridgeProxyHelper#cacheServiceInfoSnapshot(ServiceInfoSnapshot)}
   */
  @Deprecated
  public static final Map<String, ServiceInfoSnapshot> SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION = BridgeProxyHelper.SERVICE_CACHE;

  private static int lastOnlineCount = -1;

  private static ProxyServer proxyServer;

  private VelocityCloudNetHelper() {
    throw new UnsupportedOperationException();
  }

  public static int getLastOnlineCount() {
    return lastOnlineCount;
  }

  public static void addServerToVelocityPrioritySystemConfiguration(ServiceInfoSnapshot serviceInfoSnapshot,
    String name) {
    Preconditions.checkNotNull(name);
    Preconditions.checkNotNull(serviceInfoSnapshot);

    handleWithListenerInfoServerPriority(collection -> {
      for (ProxyFallbackConfiguration bungeeFallbackConfiguration : BridgeConfigurationProvider.load()
        .getBungeeFallbackConfigurations()) {
        if (bungeeFallbackConfiguration != null && bungeeFallbackConfiguration.getFallbacks() != null &&
          bungeeFallbackConfiguration.getTargetGroup() != null && Arrays
          .asList(Wrapper.getInstance().getCurrentServiceInfoSnapshot().getConfiguration().getGroups())
          .contains(bungeeFallbackConfiguration.getTargetGroup())) {
          if (!collection.contains(name) && bungeeFallbackConfiguration.getDefaultFallbackTask()
            .equals(serviceInfoSnapshot.getServiceId().getTaskName())) {
            collection.add(name);
          }
        }
      }
    });
  }

  public static void removeServerToVelocityPrioritySystemConfiguration(ServiceInfoSnapshot serviceInfoSnapshot,
    String name) {
    Preconditions.checkNotNull(name);

    handleWithListenerInfoServerPriority(collection -> collection.remove(name));
  }

  public static void handleWithListenerInfoServerPriority(Consumer<Collection<String>> listenerInfoConsumer) {
    listenerInfoConsumer.accept(proxyServer.getConfiguration().getAttemptConnectionOrder());
  }

  public static void updateServiceInfo() {
    Wrapper.getInstance().publishServiceInfoUpdate();
  }

  public static NetworkConnectionInfo createNetworkConnectionInfo(Player player) {
    return new NetworkConnectionInfo(
      player.getUniqueId(),
      player.getUsername(),
      player.getProtocolVersion().getProtocol(),
      new HostAndPort(player.getRemoteAddress()),
      new HostAndPort(proxyServer.getBoundAddress()),
      proxyServer.getConfiguration().isOnlineMode(),
      true,
      BridgeHelper.createOwnNetworkServiceInfo()
    );
  }

  public static Optional<RegisteredServer> getNextFallback(Player player) {
    return BridgeProxyHelper.getNextFallback(
      player.getUniqueId(),
      player.getCurrentServer().map(ServerConnection::getServerInfo).map(ServerInfo::getName).orElse(null),
      player.getVirtualHost().map(InetSocketAddress::getHostString).orElse(null),
      player::hasPermission
    ).map(serviceInfoSnapshot -> new VelocityPlayerFallbackEvent(player, serviceInfoSnapshot,
      serviceInfoSnapshot.getName()))
      .map(event -> proxyServer.getEventManager().fire(event))
      .map(future -> {
        try {
          return future.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
          exception.printStackTrace();
        }
        return null;
      })
      .map(VelocityPlayerFallbackEvent::getFallbackName)
      .flatMap(proxyServer::getServer);
  }

  public static CompletableFuture<ServiceInfoSnapshot> connectToFallback(Player player, String currentServer) {
    return BridgeProxyHelper.connectToFallback(player.getUniqueId(),
      currentServer,
      player.getVirtualHost().map(InetSocketAddress::getHostString).orElse(null),
      player::hasPermission,
      serviceInfoSnapshot -> {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        proxyServer.getEventManager()
          .fire(new VelocityPlayerFallbackEvent(player, serviceInfoSnapshot, serviceInfoSnapshot.getName()))
          .thenAccept(event -> {
            if (event.getFallbackName() == null) {
              future.complete(false);
              return;
            }

            Optional<RegisteredServer> optionalServer = proxyServer.getServer(event.getFallbackName());
            if (optionalServer.isPresent()) {
              player.createConnectionRequest(optionalServer.get())
                .connect()
                .thenAccept(result -> future.complete(result.isSuccessful()));
            } else {
              future.complete(false);
            }
          });

        return future;
      }
    );
  }

  public static boolean isServiceEnvironmentTypeProvidedForVelocity(ServiceInfoSnapshot serviceInfoSnapshot) {
    Preconditions.checkNotNull(serviceInfoSnapshot);
    return serviceInfoSnapshot.getServiceId().getEnvironment().isMinecraftJavaServer();
  }

  public static boolean isOnMatchingFallbackInstance(Player player) {
    return player.getCurrentServer().map(serverConnection -> {
      String currentServer = serverConnection.getServerInfo().getName();
      ServiceInfoSnapshot currentService = BridgeProxyHelper.getCachedServiceInfoSnapshot(currentServer);

      if (currentService == null) {
        return false;
      }

      return BridgeProxyHelper.filterPlayerFallbacks(
        player.getUniqueId(),
        currentServer,
        player.getVirtualHost().map(InetSocketAddress::getHostString).orElse(null),
        player::hasPermission
      ).anyMatch(proxyFallback -> proxyFallback.getTask().equals(currentService.getServiceId().getTaskName()));
    }).orElse(false);
  }

  public static boolean isFallbackServer(ServerInfo serverInfo) {
    if (serverInfo == null) {
      return false;
    }
    return BridgeProxyHelper.isFallbackService(serverInfo.getName());
  }

  public static void init() {
    BridgeProxyHelper.setMaxPlayers(proxyServer.getConfiguration().getShowMaxPlayers());
  }

  public static void initProperties(ServiceInfoSnapshot serviceInfoSnapshot) {
    lastOnlineCount = proxyServer.getPlayerCount();

    serviceInfoSnapshot.getProperties()
      .append("Online", BridgeHelper.isOnline())
      .append("Version", proxyServer.getVersion().getVersion())
      .append("Version-Vendor", proxyServer.getVersion().getVendor())
      .append("Velocity-Name", proxyServer.getVersion().getName())
      .append("Online-Count", proxyServer.getPlayerCount())
      .append("Online-Mode", proxyServer.getConfiguration().isOnlineMode())
      .append("Max-Players", BridgeProxyHelper.getMaxPlayers())
      .append("Compression-Level", proxyServer.getConfiguration().getCompressionLevel())
      .append("Connection-Timeout", proxyServer.getConfiguration().getConnectTimeout())
      .append("Players", proxyServer.getAllPlayers().stream().map(player -> new VelocityCloudNetPlayerInfo(
        player.getUniqueId(),
        player.getUsername(),
        player.getCurrentServer().isPresent() ? player.getCurrentServer().get().getServerInfo().getName() : null,
        (int) player.getPing(),
        new HostAndPort(player.getRemoteAddress())
      )).collect(Collectors.toList()))
      .append("Plugins", proxyServer.getPluginManager().getPlugins().stream().map(pluginContainer -> {
        PluginInfo pluginInfo = new PluginInfo(
          pluginContainer.getDescription().getName().orElse(null),
          pluginContainer.getDescription().getVersion().orElse(null)
        );

        pluginInfo.getProperties()
          .append("authors", pluginContainer.getDescription().getAuthors())
          .append("depends", pluginContainer.getDescription().getDependencies());

        return pluginInfo;
      }).collect(Collectors.toList()));
  }

  public static ProxyServer getProxyServer() {
    return VelocityCloudNetHelper.proxyServer;
  }

  public static void setProxyServer(ProxyServer proxyServer) {
    VelocityCloudNetHelper.proxyServer = proxyServer;
  }
}
