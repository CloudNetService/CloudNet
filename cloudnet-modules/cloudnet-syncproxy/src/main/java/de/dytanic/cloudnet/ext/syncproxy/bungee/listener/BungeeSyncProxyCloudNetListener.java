package de.dytanic.cloudnet.ext.syncproxy.bungee.listener;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceDisconnectNetworkEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceInfoUpdateEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceStartEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceStopEvent;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyConfigurationProvider;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyConstants;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyProxyLoginConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.bungee.BungeeCloudNetSyncProxyPlugin;
import de.dytanic.cloudnet.wrapper.event.service.ServiceInfoSnapshotConfigureEvent;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.UUID;

public final class BungeeSyncProxyCloudNetListener {

    @EventListener
    public void handle(ServiceInfoSnapshotConfigureEvent event) {
        if (BungeeCloudNetSyncProxyPlugin.getInstance().getProxyLoginConfiguration() != null) {
            event.getServiceInfoSnapshot().getProperties().append(SyncProxyConstants.SYNC_PROXY_SERVICE_INFO_SNAPSHOT_ONLINE_COUNT, ProxyServer.getInstance().getOnlineCount());
        }
    }

    @EventListener
    public void handle(CloudServiceInfoUpdateEvent event) {
        if (!event.getServiceInfo().getServiceId().getEnvironment().isMinecraftJavaProxy() &&
                !event.getServiceInfo().getServiceId().getEnvironment().isMinecraftBedrockProxy()) {
            return;
        }

        SyncProxyProxyLoginConfiguration syncProxyProxyLoginConfiguration = BungeeCloudNetSyncProxyPlugin.getInstance().getProxyLoginConfiguration();

        if (syncProxyProxyLoginConfiguration != null) {
            if (BungeeCloudNetSyncProxyPlugin.getInstance().inGroup(event.getServiceInfo(), syncProxyProxyLoginConfiguration) &&
                    event.getServiceInfo().getProperties().contains(SyncProxyConstants.SYNC_PROXY_SERVICE_INFO_SNAPSHOT_ONLINE_COUNT)) {
                BungeeCloudNetSyncProxyPlugin.getInstance().getOnlineCountOfProxies().put(event.getServiceInfo().getServiceId().getUniqueId(), event.getServiceInfo()
                        .getProperties().getInt(SyncProxyConstants.SYNC_PROXY_SERVICE_INFO_SNAPSHOT_ONLINE_COUNT));
            }
        }

        for (ProxiedPlayer proxiedPlayer : ProxyServer.getInstance().getPlayers()) {
            BungeeCloudNetSyncProxyPlugin.getInstance().setTabList(proxiedPlayer);
        }
    }

    @EventListener
    public void handle(CloudServiceStopEvent event) {
        this.broadcastStartStop("service-stop", event.getServiceInfo());

        if (!event.getServiceInfo().getServiceId().getEnvironment().isMinecraftJavaProxy() &&
                !event.getServiceInfo().getServiceId().getEnvironment().isMinecraftBedrockProxy()) {
            return;
        }

        BungeeCloudNetSyncProxyPlugin.getInstance().getOnlineCountOfProxies().remove(event.getServiceInfo().getServiceId().getUniqueId());
    }

    @EventListener
    public void handle(CloudServiceStartEvent event) {
        this.broadcastStartStop("service-start", event.getServiceInfo());
    }

    private void broadcastStartStop(String key, ServiceInfoSnapshot serviceInfoSnapshot) {
        SyncProxyConfiguration configuration = SyncProxyConfigurationProvider.load();
        if (configuration != null && configuration.showIngameServicesStartStopMessages()) {
            String message = ChatColor.translateAlternateColorCodes('&', configuration.getMessages().get(key).replace("%service%", serviceInfoSnapshot.getServiceId().getName()));
            for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
                if (player.hasPermission("cloudnet.syncproxy.notify")) {
                    player.sendMessage(TextComponent.fromLegacyText(message));
                }
            }
        }
    }

    @EventListener
    public void handle(CloudServiceDisconnectNetworkEvent event) {
        if (!event.getServiceInfo().getServiceId().getEnvironment().isMinecraftJavaProxy() &&
                !event.getServiceInfo().getServiceId().getEnvironment().isMinecraftBedrockProxy()) {
            return;
        }

        BungeeCloudNetSyncProxyPlugin.getInstance().getOnlineCountOfProxies().remove(event.getServiceInfo().getServiceId().getUniqueId());
    }

    @EventListener
    public void handle(ChannelMessageReceiveEvent event) {
        if (!event.getChannel().equals(SyncProxyConstants.SYNC_PROXY_CHANNEL_NAME)) {
            return;
        }

        if (SyncProxyConstants.SYNC_PROXY_UPDATE_CONFIGURATION.equals(event.getMessage().toLowerCase())) {
            SyncProxyConfiguration syncProxyConfiguration = event.getData().get("syncProxyConfiguration", SyncProxyConfiguration.TYPE);

            if (syncProxyConfiguration != null) {
                SyncProxyConfigurationProvider.setLocal(syncProxyConfiguration);
            }

            handlePlayerNotWhitelisted();
        }
    }


    private void handlePlayerNotWhitelisted() {
        SyncProxyProxyLoginConfiguration syncProxyProxyLoginConfiguration = BungeeCloudNetSyncProxyPlugin.getInstance().getProxyLoginConfiguration();

        if (syncProxyProxyLoginConfiguration != null) {
            for (ProxiedPlayer proxiedPlayer : ProxyServer.getInstance().getPlayers()) {
                if (syncProxyProxyLoginConfiguration.isMaintenance() &&
                        syncProxyProxyLoginConfiguration.getWhitelist() != null &&
                        !syncProxyProxyLoginConfiguration.getWhitelist().contains(proxiedPlayer.getName())) {
                    UUID uniqueId = proxiedPlayer.getUniqueId();

                    if (syncProxyProxyLoginConfiguration.getWhitelist().contains(uniqueId.toString())) {
                        continue;
                    }

                    if (!proxiedPlayer.hasPermission("cloudnet.syncproxy.maintenance")) {
                        proxiedPlayer.disconnect(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&',
                                SyncProxyConfigurationProvider.load().getMessages().get("player-login-not-whitelisted")))
                        );
                    }
                }
            }
        }
    }

}