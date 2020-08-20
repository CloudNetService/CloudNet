package de.dytanic.cloudnet.ext.syncproxy.bungee;


import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.player.ICloudPlayer;
import de.dytanic.cloudnet.ext.syncproxy.AbstractSyncProxyManagement;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyProxyLoginConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyTabList;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class BungeeSyncProxyManagement extends AbstractSyncProxyManagement {

    private final Plugin plugin;

    public BungeeSyncProxyManagement(Plugin plugin) {
        this.plugin = plugin;

        super.setSyncProxyConfiguration(SyncProxyConfiguration.getConfigurationFromNode());
        super.scheduleTabList();
    }

    @Override
    protected void scheduleNative(Runnable runnable, long millis) {
        ProxyServer.getInstance().getScheduler().schedule(this.plugin, runnable, millis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void updateTabList() {
        if (super.tabListEntryIndex.get() == -1) {
            return;
        }

        ProxyServer.getInstance().getPlayers().forEach(this::updateTabList);
    }

    public void updateTabList(ProxiedPlayer proxiedPlayer) {
        proxiedPlayer.setTabHeader(
                TextComponent.fromLegacyText(super.tabListHeader != null ?
                        this.replaceTabListItem(proxiedPlayer, ChatColor.translateAlternateColorCodes('&', super.tabListHeader))
                        : ""
                ),
                TextComponent.fromLegacyText(super.tabListFooter != null ?
                        this.replaceTabListItem(proxiedPlayer, ChatColor.translateAlternateColorCodes('&', super.tabListFooter))
                        : ""
                )
        );
    }

    private String replaceTabListItem(ProxiedPlayer proxiedPlayer, String input) {
        ICloudPlayer cloudPlayer = super.playerManager.getOnlinePlayer(proxiedPlayer.getUniqueId());
        input = input
                .replace("%server%", proxiedPlayer.getServer() != null ? proxiedPlayer.getServer().getInfo().getName() : "")
                .replace("%task%", cloudPlayer != null ? cloudPlayer.getConnectedService().getTaskName() : "")
                .replace("%online_players%", String.valueOf(super.loginConfiguration != null ? super.getSyncProxyOnlineCount() : ProxyServer.getInstance().getOnlineCount()))
                .replace("%max_players%", String.valueOf(super.loginConfiguration != null ? super.loginConfiguration.getMaxPlayers() : proxiedPlayer.getPendingConnection().getListener().getMaxPlayers()))
                .replace("%name%", proxiedPlayer.getName())
                .replace("%ping%", String.valueOf(proxiedPlayer.getPing()));

        return SyncProxyTabList.replaceTabListItem(input, proxiedPlayer.getUniqueId());
    }

    @Override
    protected void checkWhitelist() {
        SyncProxyProxyLoginConfiguration syncProxyProxyLoginConfiguration = super.getLoginConfiguration();

        if (syncProxyProxyLoginConfiguration != null) {
            for (ProxiedPlayer proxiedPlayer : ProxyServer.getInstance().getPlayers()) {
                if (syncProxyProxyLoginConfiguration.isMaintenance()
                        && syncProxyProxyLoginConfiguration.getWhitelist() != null
                        && !syncProxyProxyLoginConfiguration.getWhitelist().contains(proxiedPlayer.getName())) {
                    UUID uniqueId = proxiedPlayer.getUniqueId();

                    if (syncProxyProxyLoginConfiguration.getWhitelist().contains(uniqueId.toString())) {
                        continue;
                    }

                    if (!proxiedPlayer.hasPermission("cloudnet.syncproxy.maintenance")) {
                        proxiedPlayer.disconnect(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&',
                                super.getSyncProxyConfiguration().getMessages().get("player-login-not-whitelisted")))
                        );
                    }
                }
            }
        }
    }

    @Override
    public void broadcastServiceStateChange(String key, ServiceInfoSnapshot serviceInfoSnapshot) {
        if (super.syncProxyConfiguration != null && super.syncProxyConfiguration.showIngameServicesStartStopMessages()) {
            String message = ChatColor.translateAlternateColorCodes('&', super.getServiceStateChangeMessage(key, serviceInfoSnapshot));

            for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
                if (player.hasPermission("cloudnet.syncproxy.notify")) {
                    player.sendMessage(TextComponent.fromLegacyText(message));
                }
            }
        }
    }

}
