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

package de.dytanic.cloudnet.ext.bridge.waterdogpe;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.PluginInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import de.dytanic.cloudnet.ext.bridge.proxy.BridgeProxyHelper;
import de.dytanic.cloudnet.ext.bridge.waterdogpe.event.WaterdogPEPlayerFallbackEvent;
import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.WaterdogPE;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.network.session.LoginData;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class WaterdogPECloudNetHelper {

  /**
   * @deprecated use {@link BridgeProxyHelper#getCachedServiceInfoSnapshot(String)} or {@link
   * BridgeProxyHelper#cacheServiceInfoSnapshot(ServiceInfoSnapshot)}
   */
  @Deprecated
  public static final Map<String, ServiceInfoSnapshot> SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION = BridgeProxyHelper.SERVICE_CACHE;
  private static int lastOnlineCount = -1;

  private WaterdogPECloudNetHelper() {
    throw new UnsupportedOperationException();
  }

  public static int getLastOnlineCount() {
    return lastOnlineCount;
  }

  public static boolean isOnMatchingFallbackInstance(ProxiedPlayer player) {
    String currentServer = player.getServerInfo() == null ? null : player.getServerInfo().getServerName();

    if (currentServer != null) {
      ServiceInfoSnapshot currentService = BridgeProxyHelper.getCachedServiceInfoSnapshot(currentServer);

      if (currentService != null) {
        return BridgeProxyHelper.filterPlayerFallbacks(
          player.getUniqueId(),
          currentServer,
          player::hasPermission
        ).anyMatch(proxyFallback ->
          proxyFallback.getTask().equals(currentService.getServiceId().getTaskName()));
      }
    }

    return false;
  }

  public static boolean isFallbackServer(ServerInfo serverInfo) {
    if (serverInfo == null) {
      return false;
    }
    return BridgeProxyHelper.isFallbackService(serverInfo.getServerName());
  }

  public static Optional<ServerInfo> getNextFallback(ProxiedPlayer player, ServerInfo currentServer) {
    return BridgeProxyHelper.getNextFallback(
      player.getUniqueId(),
      currentServer != null ? currentServer.getServerName() : null,
      player::hasPermission
    )
      .map(serviceInfoSnapshot -> {
        WaterdogPEPlayerFallbackEvent event = new WaterdogPEPlayerFallbackEvent(player, serviceInfoSnapshot,
          serviceInfoSnapshot.getName());
        ProxyServer.getInstance().getEventManager().callEvent(event);
        return event;
      })
      .map(WaterdogPEPlayerFallbackEvent::getFallbackName)
      .map(fallback -> ProxyServer.getInstance().getServerInfo(fallback));
  }

  public static CompletableFuture<ServiceInfoSnapshot> connectToFallback(ProxiedPlayer player, String currentServer) {
    return BridgeProxyHelper.connectToFallback(player.getUniqueId(), currentServer,
      player::hasPermission,
      serviceInfoSnapshot -> {
        WaterdogPEPlayerFallbackEvent event = new WaterdogPEPlayerFallbackEvent(player, serviceInfoSnapshot,
          serviceInfoSnapshot.getName());
        ProxyServer.getInstance().getEventManager().callEvent(event);
        if (event.getFallbackName() == null) {
          return CompletableFuture.completedFuture(false);
        }

        ServerInfo serverInfo = ProxyServer.getInstance().getServerInfo(event.getFallbackName());
        if (serverInfo == null) {
          return CompletableFuture.completedFuture(false);
        }

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        player.connect(serverInfo);
        future.complete(true);
        return future;
      }
    );
  }

  public static void initProperties(ServiceInfoSnapshot serviceInfoSnapshot) {
    Preconditions.checkNotNull(serviceInfoSnapshot);

    lastOnlineCount = ProxyServer.getInstance().getPlayers().size();

    serviceInfoSnapshot.getProperties()
      .append("Online", BridgeHelper.isOnline())
      .append("Version", WaterdogPE.version().baseVersion())
      .append("Protocol-Version", WaterdogPE.version().latestProtocolVersion())
      .append("Online-Count", ProxyServer.getInstance().getPlayers().size())
      .append("WaterdogPE-Name", "WaterdogPE")
      .append("Players",
        ProxyServer.getInstance().getPlayers().values().stream().map(proxiedPlayer -> new WaterdogPECloudNetPlayerInfo(
          proxiedPlayer.getUniqueId(),
          proxiedPlayer.getName(),
          proxiedPlayer.getServerInfo() != null ? proxiedPlayer.getServerInfo().getServerName() : null,
          (int) proxiedPlayer.getPing(),
          new HostAndPort(proxiedPlayer.getLoginData().getAddress())
        )).collect(Collectors.toList()))
      .append("Plugins", ProxyServer.getInstance().getPluginManager().getPlugins().stream().map(plugin -> {
        PluginInfo pluginInfo = new PluginInfo(plugin.getDescription().getName(), plugin.getDescription().getVersion());

        pluginInfo.getProperties()
          .append("author", plugin.getDescription().getAuthor())
          .append("main-class", plugin.getDescription().getMain())
          .append("depends", plugin.getDescription().getDepends())
        ;

        return pluginInfo;
      }).collect(Collectors.toList()))
    ;
  }

  public static NetworkConnectionInfo createNetworkConnectionInfo(LoginData loginData) {
    return BridgeHelper.createNetworkConnectionInfo(
      loginData.getUuid(),
      loginData.getDisplayName(),
      loginData.getProtocol().getProtocol(),
      new HostAndPort(loginData.getAddress()),
      new HostAndPort(ProxyServer.getInstance().getConfiguration().getBindAddress()),
      ProxyServer.getInstance().getConfiguration().isOnlineMode(),
      loginData.getProtocol().getProtocol() < WaterdogPE.version().latestProtocolVersion(),
      BridgeHelper.createOwnNetworkServiceInfo()
    );
  }

}
