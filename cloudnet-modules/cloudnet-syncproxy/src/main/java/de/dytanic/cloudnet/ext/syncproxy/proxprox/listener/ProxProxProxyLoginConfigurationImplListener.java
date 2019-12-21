package de.dytanic.cloudnet.ext.syncproxy.proxprox.listener;

import de.dytanic.cloudnet.ext.syncproxy.SyncProxyConfigurationProvider;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyMotd;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyProxyLoginConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.proxprox.ProxProxCloudNetSyncProxyPlugin;
import de.dytanic.cloudnet.wrapper.Wrapper;
import io.gomint.proxprox.api.ChatColor;
import io.gomint.proxprox.api.event.PlayerLoginEvent;
import io.gomint.proxprox.api.event.ProxyPingEvent;
import io.gomint.proxprox.api.plugin.event.EventHandler;
import io.gomint.proxprox.api.plugin.event.Listener;

import java.util.Random;

public final class ProxProxProxyLoginConfigurationImplListener implements Listener {

    @EventHandler
    public void handle(ProxyPingEvent event) {
        SyncProxyProxyLoginConfiguration syncProxyProxyLoginConfiguration = ProxProxCloudNetSyncProxyPlugin.getInstance().getProxyLoginConfiguration();

        if (syncProxyProxyLoginConfiguration != null) {
            SyncProxyMotd syncProxyMotd = null;
            Random random = new Random();

            if (syncProxyProxyLoginConfiguration.isMaintenance()) {
                if (syncProxyProxyLoginConfiguration.getMaintenanceMotds() != null && !syncProxyProxyLoginConfiguration.getMaintenanceMotds().isEmpty()) {
                    syncProxyMotd = syncProxyProxyLoginConfiguration.getMaintenanceMotds().get(random.nextInt(
                            syncProxyProxyLoginConfiguration.getMaintenanceMotds().size()));
                }
            } else {
                if (syncProxyProxyLoginConfiguration.getMotds() != null && !syncProxyProxyLoginConfiguration.getMotds().isEmpty()) {
                    syncProxyMotd = syncProxyProxyLoginConfiguration.getMotds().get(random.nextInt(
                            syncProxyProxyLoginConfiguration.getMotds().size()));
                }
            }

            if (syncProxyMotd != null) {
                int onlinePlayers = ProxProxCloudNetSyncProxyPlugin.getInstance().getSyncProxyOnlineCount();

                event.setOnlinePlayers(onlinePlayers);
                event.setMaxPlayers(syncProxyMotd.isAutoSlot() ?
                        (syncProxyMotd.getAutoSlotMaxPlayersDistance() + onlinePlayers) :
                        syncProxyProxyLoginConfiguration.getMaxPlayers());

                event.setMotd((syncProxyMotd.getFirstLine() + "\n" + syncProxyMotd.getSecondLine())
                        .replace("%proxy%", Wrapper.getInstance().getServiceId().getName())
                        .replace("%proxy_uniqueId%", String.valueOf(Wrapper.getInstance().getServiceId().getUniqueId()))
                        .replace("%task%", Wrapper.getInstance().getServiceId().getTaskName())
                        .replace("%node%", Wrapper.getInstance().getServiceId().getNodeUniqueId()));
            }
        }
    }

    @EventHandler
    public void handle(PlayerLoginEvent event) {
        SyncProxyProxyLoginConfiguration syncProxyProxyLoginConfiguration = ProxProxCloudNetSyncProxyPlugin.getInstance().getProxyLoginConfiguration();

        if (syncProxyProxyLoginConfiguration != null) {
            if (syncProxyProxyLoginConfiguration.isMaintenance() && syncProxyProxyLoginConfiguration.getWhitelist() != null) {
                if (syncProxyProxyLoginConfiguration.getWhitelist().contains(event.getPlayer().getName()) ||
                        syncProxyProxyLoginConfiguration.getWhitelist().contains(event.getPlayer().getUUID().toString()) ||
                        event.getPlayer().hasPermission("cloudnet.syncproxy.maintenance")) {
                    return;
                }

                event.deny(ChatColor.toANSI((SyncProxyConfigurationProvider.load().getMessages().get("player-login-not-whitelisted"))));
                return;
            }

            if (ProxProxCloudNetSyncProxyPlugin.getInstance().getSyncProxyOnlineCount() >= syncProxyProxyLoginConfiguration.getMaxPlayers() &&
                    !event.getPlayer().hasPermission("cloudnet.syncproxy.fulljoin")) {
                event.deny(ChatColor.toANSI(SyncProxyConfigurationProvider.load().getMessages()
                        .getOrDefault("player-login-full-server", "&cThe network is currently full. You need extra permissions to enter the network")));
            }
        }
    }
}