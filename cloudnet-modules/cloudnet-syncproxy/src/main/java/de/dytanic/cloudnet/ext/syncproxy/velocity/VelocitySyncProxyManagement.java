package de.dytanic.cloudnet.ext.syncproxy.velocity;


import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.syncproxy.AbstractSyncProxyManagement;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyProxyLoginConfiguration;
import net.kyori.text.TextComponent;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

// TODO: implement
public class VelocitySyncProxyManagement extends AbstractSyncProxyManagement {

    private ProxyServer proxyServer;

    private Object plugin;

    public VelocitySyncProxyManagement(ProxyServer proxyServer, Object plugin) {
        this.proxyServer = proxyServer;
        this.plugin = plugin;
    }

    @Override
    protected void scheduleNative(Runnable runnable, long millis) {
        this.proxyServer.getScheduler().buildTask(this.plugin, runnable).delay(millis, TimeUnit.MILLISECONDS).schedule();
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
            String message = configuration.getMessages().get(key).replace("%service%", serviceInfoSnapshot.getServiceId().getName()).replace("&", "§");

            for (Player player : this.proxyServer.getAllPlayers()) {
                if (player.hasPermission("cloudnet.syncproxy.notify")) {
                    player.sendMessage(TextComponent.of(message));
                }
            }
        }
    }

}
