package de.dytanic.cloudnet.ext.syncproxy.bungee;


import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.syncproxy.AbstractSyncProxyManagement;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyProxyLoginConfiguration;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

// TODO: implement
public class BungeeSyncProxyManagement extends AbstractSyncProxyManagement {

    private Plugin plugin;

    public BungeeSyncProxyManagement(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void scheduleNative(Runnable runnable, long millis) {
        ProxyServer.getInstance().getScheduler().schedule(this.plugin, runnable, millis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void updateTabList() {

    }

    @Override
    protected String replaceTabListItem(UUID playerUniqueId, SyncProxyProxyLoginConfiguration syncProxyProxyLoginConfiguration, String input) {
        return null;
    }

    @Override
    protected void checkWhitelist() {

    }

    @Override
    public void broadcastServiceStateChange(String key, ServiceInfoSnapshot serviceInfoSnapshot) {
        SyncProxyConfiguration configuration = super.getSyncProxyConfiguration();

        if (configuration != null && configuration.showIngameServicesStartStopMessages()) {
            String message = ChatColor.translateAlternateColorCodes('&', configuration.getMessages().get(key).replace("%service%", serviceInfoSnapshot.getServiceId().getName()));
            for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
                if (player.hasPermission("cloudnet.syncproxy.notify")) {
                    player.sendMessage(TextComponent.fromLegacyText(message));
                }
            }
        }
    }

}
