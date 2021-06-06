package de.dytanic.cloudnet.ext.bridge.bungee.listener;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.bungee.BungeeCloudNetBridgePlugin;
import de.dytanic.cloudnet.ext.bridge.bungee.BungeeCloudNetHelper;
import de.dytanic.cloudnet.ext.bridge.proxy.BridgeProxyHelper;
import java.util.concurrent.TimeUnit;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public final class BungeePlayerListener implements Listener {

  private final BungeeCloudNetBridgePlugin plugin;

  public BungeePlayerListener(BungeeCloudNetBridgePlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void handle(LoginEvent event) {
    String kickReason = BridgeHelper
      .sendChannelMessageProxyLoginRequest(BungeeCloudNetHelper.createNetworkConnectionInfo(event.getConnection()));
    if (kickReason != null) {
      event.setCancelled(true);
      event.setCancelReason(TextComponent.fromLegacyText(kickReason));
    }
  }

  @EventHandler
  public void handle(PostLoginEvent event) {
    BridgeHelper.sendChannelMessageProxyLoginSuccess(
      BungeeCloudNetHelper.createNetworkConnectionInfo(event.getPlayer().getPendingConnection()));
    BridgeHelper.updateServiceInfo();
  }

  @EventHandler
  public void handle(ServerSwitchEvent event) {
    ServiceInfoSnapshot serviceInfoSnapshot = BridgeProxyHelper
      .getCachedServiceInfoSnapshot(event.getPlayer().getServer().getInfo().getName());

    if (serviceInfoSnapshot != null) {
      BridgeHelper.sendChannelMessageProxyServerSwitch(
        BungeeCloudNetHelper.createNetworkConnectionInfo(event.getPlayer().getPendingConnection()),
        BridgeHelper.createNetworkServiceInfo(serviceInfoSnapshot)
      );
    }
  }

  @EventHandler
  public void handle(ServerConnectEvent event) {
    ProxiedPlayer proxiedPlayer = event.getPlayer();

    ServiceInfoSnapshot serviceInfoSnapshot = BridgeProxyHelper
      .getCachedServiceInfoSnapshot(event.getTarget().getName());

    if (serviceInfoSnapshot != null) {
      BridgeHelper.sendChannelMessageProxyServerConnectRequest(
        BungeeCloudNetHelper.createNetworkConnectionInfo(proxiedPlayer.getPendingConnection()),
        BridgeHelper.createNetworkServiceInfo(serviceInfoSnapshot)
      );
    }
  }

  @EventHandler
  public void handle(ServerKickEvent event) {
    if (event.getPlayer().isConnected()) {
      ServerInfo kickFrom = event.getKickedFrom();

      if (kickFrom == null) {
        event.getPlayer().disconnect(event.getKickReasonComponent());
        event.setCancelled(true);
        return;
      }
      BridgeProxyHelper.handleConnectionFailed(event.getPlayer().getUniqueId(), kickFrom.getName());

      BungeeCloudNetHelper.getNextFallback(event.getPlayer(), kickFrom).ifPresent(serverInfo -> {
        event.setCancelled(true);
        event.setCancelServer(serverInfo);
        event.getPlayer().sendMessage(event.getKickReasonComponent());
      });
    }
  }

  @EventHandler
  public void handle(PlayerDisconnectEvent event) {
    BridgeHelper.sendChannelMessageProxyDisconnect(
      BungeeCloudNetHelper.createNetworkConnectionInfo(event.getPlayer().getPendingConnection()));
    BridgeProxyHelper.clearFallbackProfile(event.getPlayer().getUniqueId());

    ProxyServer.getInstance().getScheduler()
      .schedule(this.plugin, BridgeHelper::updateServiceInfo, 50, TimeUnit.MILLISECONDS);
  }
}
