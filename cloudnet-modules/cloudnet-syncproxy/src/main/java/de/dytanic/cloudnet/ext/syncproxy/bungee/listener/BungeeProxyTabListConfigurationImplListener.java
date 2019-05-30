package de.dytanic.cloudnet.ext.syncproxy.bungee.listener;

import de.dytanic.cloudnet.ext.syncproxy.SyncProxyTabListConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.bungee.BungeeCloudNetSyncProxyPlugin;
import de.dytanic.cloudnet.wrapper.Wrapper;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public final class BungeeProxyTabListConfigurationImplListener implements
  Listener {

  @EventHandler
  public void handle(PostLoginEvent event) {
    SyncProxyTabListConfiguration syncProxyTabListConfiguration = BungeeCloudNetSyncProxyPlugin
      .getInstance().getTabListConfiguration();

    if (syncProxyTabListConfiguration != null &&
      ProxyServer.getInstance().getPluginManager()
        .getPlugin("CloudNet-Bridge") == null) {
      Wrapper.getInstance().publishServiceInfoUpdate();
    }
  }

  @EventHandler
  public void handle(ServerConnectedEvent event) {
    SyncProxyTabListConfiguration syncProxyTabListConfiguration = BungeeCloudNetSyncProxyPlugin
      .getInstance().getTabListConfiguration();

    if (syncProxyTabListConfiguration != null) {
      BungeeCloudNetSyncProxyPlugin.getInstance().setTabList(event.getPlayer());
    }
  }

  @EventHandler
  public void handle(PlayerDisconnectEvent event) {
    SyncProxyTabListConfiguration syncProxyTabListConfiguration = BungeeCloudNetSyncProxyPlugin
      .getInstance().getTabListConfiguration();

    if (syncProxyTabListConfiguration != null &&
      ProxyServer.getInstance().getPluginManager()
        .getPlugin("CloudNet-Bridge") == null) {
      Wrapper.getInstance().publishServiceInfoUpdate();
    }
  }
}