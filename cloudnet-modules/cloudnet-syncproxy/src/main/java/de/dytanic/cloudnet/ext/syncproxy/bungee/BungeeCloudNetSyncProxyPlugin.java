package de.dytanic.cloudnet.ext.syncproxy.bungee;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.syncproxy.AbstractSyncProxyManagement;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyCloudNetListener;
import de.dytanic.cloudnet.ext.syncproxy.bungee.listener.BungeeSyncProxyPlayerListener;
import de.dytanic.cloudnet.wrapper.Wrapper;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

public final class BungeeCloudNetSyncProxyPlugin extends Plugin {

  @Override
  public void onEnable() {
    BungeeSyncProxyManagement syncProxyManagement = new BungeeSyncProxyManagement(this);
    CloudNetDriver.getInstance().getServicesRegistry()
      .registerService(AbstractSyncProxyManagement.class, "BungeeSyncProxyManagement", syncProxyManagement);

    CloudNetDriver.getInstance().getEventManager().registerListener(new SyncProxyCloudNetListener(syncProxyManagement));

    ProxyServer.getInstance().getPluginManager()
      .registerListener(this, new BungeeSyncProxyPlayerListener(syncProxyManagement));
  }

  @Override
  public void onDisable() {
    CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
    Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());

    CloudNetDriver.getInstance().getServicesRegistry()
      .unregisterService(AbstractSyncProxyManagement.class, "BungeeSyncProxyManagement");
  }

}
