package de.dytanic.cloudnet.ext.syncproxy.velocity;


import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.proxy.BridgeProxyHelper;
import de.dytanic.cloudnet.ext.syncproxy.AbstractSyncProxyManagement;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyTabList;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;

import java.util.concurrent.TimeUnit;

public class VelocitySyncProxyManagement extends AbstractSyncProxyManagement {

    private final ProxyServer proxyServer;

    private final Object plugin;

    public VelocitySyncProxyManagement(ProxyServer proxyServer, Object plugin) {
        this.proxyServer = proxyServer;
        this.plugin = plugin;

        super.setSyncProxyConfiguration(SyncProxyConfiguration.getConfigurationFromNode());
    }

    @Override
    protected void schedule(Runnable runnable, long millis) {
        this.proxyServer.getScheduler().buildTask(this.plugin, runnable).delay(millis, TimeUnit.MILLISECONDS).schedule();
    }

    @Override
    public void updateTabList() {
        if (super.tabListEntryIndex.get() == -1) {
            return;
        }

        this.proxyServer.getAllPlayers().forEach(this::updateTabList);
    }

    public void updateTabList(Player player) {
        if (super.tabListEntryIndex.get() == -1) {
            return;
        }

        player.getTabList().setHeaderAndFooter(
                LegacyComponentSerializer.legacyLinking().deserialize(super.tabListHeader != null ? this.replaceTabListItem(player, super.tabListHeader) : ""),
                LegacyComponentSerializer.legacyLinking().deserialize(super.tabListFooter != null ? this.replaceTabListItem(player, super.tabListFooter) : "")
        );
    }

    private String replaceTabListItem(Player player, String input) {
        String taskName = player.getCurrentServer()
                .map(serverConnection -> BridgeProxyHelper.getCachedServiceInfoSnapshot(serverConnection.getServerInfo().getName()))
                .map(serviceInfoSnapshot -> serviceInfoSnapshot.getServiceId().getTaskName())
                .orElse("");
        String serverName = player.getCurrentServer().map(serverConnection -> serverConnection.getServerInfo().getName()).orElse("");
        input = input
                .replace("%server%", serverName)
                .replace("%task%", taskName)
                .replace("%online_players%", String.valueOf(super.loginConfiguration != null ? super.getSyncProxyOnlineCount() : this.proxyServer.getPlayerCount()))
                .replace("%max_players%", String.valueOf(super.loginConfiguration != null ? super.loginConfiguration.getMaxPlayers() : this.proxyServer.getConfiguration().getShowMaxPlayers()))
                .replace("%name%", player.getUsername())
                .replace("%ping%", String.valueOf(player.getPing()));

        return SyncProxyTabList.replaceTabListItem(input, player.getUniqueId());
    }


    @Override
    protected void checkWhitelist() {
        if (super.loginConfiguration != null) {
            for (Player player : this.proxyServer.getAllPlayers()) {
                if (super.loginConfiguration.isMaintenance()
                        && super.loginConfiguration.getWhitelist() != null
                        && !super.loginConfiguration.getWhitelist().contains(player.getUsername())
                        && !super.loginConfiguration.getWhitelist().contains(player.getUniqueId().toString())
                        && !player.hasPermission("cloudnet.syncproxy.maintenance")) {
                    player.disconnect(LegacyComponentSerializer.legacyLinking().deserialize(super.syncProxyConfiguration.getMessages().get("player-login-not-whitelisted").replace("&", "§")));
                }
            }
        }
    }

    @Override
    public void broadcastServiceStateChange(String key, ServiceInfoSnapshot serviceInfoSnapshot) {
        if (super.syncProxyConfiguration != null && super.syncProxyConfiguration.showIngameServicesStartStopMessages()) {
            String message = super.getServiceStateChangeMessage(key, serviceInfoSnapshot).replace("&", "§");

            for (Player player : this.proxyServer.getAllPlayers()) {
                if (player.hasPermission("cloudnet.syncproxy.notify")) {
                    player.sendMessage(LegacyComponentSerializer.legacyLinking().deserialize(message));
                }
            }
        }
    }

}
