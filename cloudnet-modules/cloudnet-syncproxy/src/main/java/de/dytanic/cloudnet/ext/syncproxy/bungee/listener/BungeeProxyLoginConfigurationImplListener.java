package de.dytanic.cloudnet.ext.syncproxy.bungee.listener;

import de.dytanic.cloudnet.ext.syncproxy.SyncProxyConfigurationProvider;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyMotd;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyProxyLoginConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.bungee.BungeeCloudNetSyncProxyPlugin;
import de.dytanic.cloudnet.ext.syncproxy.bungee.util.LoginPendingConnectionCommandSender;
import de.dytanic.cloudnet.wrapper.Wrapper;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.Random;
import java.util.UUID;

public final class BungeeProxyLoginConfigurationImplListener implements Listener {

    private static final Random RANDOM = new Random();

    @EventHandler
    public void handle(ProxyPingEvent event) {
        SyncProxyProxyLoginConfiguration syncProxyProxyLoginConfiguration = BungeeCloudNetSyncProxyPlugin.getInstance().getProxyLoginConfiguration();

        if (syncProxyProxyLoginConfiguration != null) {
            SyncProxyMotd syncProxyMotd = null;

            if (syncProxyProxyLoginConfiguration.isMaintenance()) {
                if (syncProxyProxyLoginConfiguration.getMaintenanceMotds() != null && !syncProxyProxyLoginConfiguration.getMaintenanceMotds().isEmpty()) {
                    syncProxyMotd = syncProxyProxyLoginConfiguration.getMaintenanceMotds().get(RANDOM.nextInt(
                            syncProxyProxyLoginConfiguration.getMaintenanceMotds().size()));
                }
            } else {
                if (syncProxyProxyLoginConfiguration.getMotds() != null && !syncProxyProxyLoginConfiguration.getMotds().isEmpty()) {
                    syncProxyMotd = syncProxyProxyLoginConfiguration.getMotds().get(RANDOM.nextInt(
                            syncProxyProxyLoginConfiguration.getMotds().size()));
                }
            }

            if (syncProxyMotd != null) {
                String protocolText = syncProxyMotd.getProtocolText();

                String motd = ChatColor.translateAlternateColorCodes('&', syncProxyMotd.getFirstLine() + "\n" + syncProxyMotd.getSecondLine())
                        .replace("%proxy%", Wrapper.getInstance().getServiceId().getName())
                        .replace("%proxy_uniqueId%", String.valueOf(Wrapper.getInstance().getServiceId().getUniqueId()))
                        .replace("%task%", Wrapper.getInstance().getServiceId().getTaskName())
                        .replace("%node%", Wrapper.getInstance().getServiceId().getNodeUniqueId());

                int onlinePlayers = BungeeCloudNetSyncProxyPlugin.getInstance().getSyncProxyOnlineCount();

                int maxPlayers = syncProxyMotd.isAutoSlot() ?
                        (syncProxyMotd.getAutoSlotMaxPlayersDistance() + onlinePlayers) :
                        syncProxyProxyLoginConfiguration.getMaxPlayers();

                ServerPing.PlayerInfo[] playerInfo = new ServerPing.PlayerInfo[syncProxyMotd.getPlayerInfo() != null ? syncProxyMotd.getPlayerInfo().length : 0];
                for (int i = 0; i < playerInfo.length; i++) {
                    playerInfo[i] = new ServerPing.PlayerInfo(ChatColor.translateAlternateColorCodes('&', syncProxyMotd.getPlayerInfo()[i]), UUID.randomUUID().toString());
                }

                ServerPing serverPing = new ServerPing(
                        new ServerPing.Protocol(ChatColor.translateAlternateColorCodes('&',
                                (protocolText == null ? event.getResponse().getVersion().getName() : protocolText)
                                        .replace("%proxy%", Wrapper.getInstance().getServiceId().getName())
                                        .replace("%proxy_uniqueId%", String.valueOf(Wrapper.getInstance().getServiceId().getUniqueId()))
                                        .replace("%task%", Wrapper.getInstance().getServiceId().getTaskName())
                                        .replace("%node%", Wrapper.getInstance().getServiceId().getNodeUniqueId())
                                        .replace("%online_players%", String.valueOf(onlinePlayers))
                                        .replace("%max_players%", String.valueOf(maxPlayers))
                        ),
                                (protocolText == null ? event.getResponse().getVersion().getProtocol() : 1)),
                        new ServerPing.Players(maxPlayers, onlinePlayers, playerInfo),
                        motd,
                        event.getResponse().getFaviconObject()
                );

                event.setResponse(serverPing);
            }
        }
    }

    @EventHandler
    public void handle(LoginEvent event) {
        SyncProxyProxyLoginConfiguration syncProxyProxyLoginConfiguration = BungeeCloudNetSyncProxyPlugin.getInstance().getProxyLoginConfiguration();

        if (syncProxyProxyLoginConfiguration != null) {
            LoginPendingConnectionCommandSender loginEventCommandSender = new LoginPendingConnectionCommandSender(event.getConnection());

            if (syncProxyProxyLoginConfiguration.isMaintenance() && syncProxyProxyLoginConfiguration.getWhitelist() != null) {
                if (syncProxyProxyLoginConfiguration.getWhitelist().contains(event.getConnection().getName())) {
                    return;
                }

                UUID uniqueId = event.getConnection().getUniqueId();

                if ((uniqueId != null && syncProxyProxyLoginConfiguration.getWhitelist().contains(uniqueId.toString())) ||
                        loginEventCommandSender.hasPermission("cloudnet.syncproxy.maintenance")) {
                    return;
                }

                event.setCancelled(true);
                event.setCancelReason(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&',
                        SyncProxyConfigurationProvider.load().getMessages().get("player-login-not-whitelisted"))
                ));
                return;
            }

            if (BungeeCloudNetSyncProxyPlugin.getInstance().getSyncProxyOnlineCount() >= syncProxyProxyLoginConfiguration.getMaxPlayers() &&
                    !loginEventCommandSender.hasPermission("cloudnet.syncproxy.fullljoin")) {
                event.setCancelled(true);
                event.setCancelReason(ChatColor.translateAlternateColorCodes('&', SyncProxyConfigurationProvider.load().getMessages()
                        .getOrDefault("player-login-full-server", "&cThe network is currently full. You need extra permissions to enter the network")));
            }
        }
    }

}