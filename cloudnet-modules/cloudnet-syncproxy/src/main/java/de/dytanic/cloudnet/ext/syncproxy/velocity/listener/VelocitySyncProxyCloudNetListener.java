package de.dytanic.cloudnet.ext.syncproxy.velocity.listener;

import com.velocitypowered.api.proxy.Player;
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
import de.dytanic.cloudnet.ext.syncproxy.velocity.VelocityCloudNetSyncProxyPlugin;
import de.dytanic.cloudnet.wrapper.event.service.ServiceInfoSnapshotConfigureEvent;
import net.kyori.text.TextComponent;

public final class VelocitySyncProxyCloudNetListener {

    @EventListener
    public void handle(ServiceInfoSnapshotConfigureEvent event) {
        if (VelocityCloudNetSyncProxyPlugin.getInstance().getProxyLoginConfiguration() != null) {
            event.getServiceInfoSnapshot().getProperties().append(SyncProxyConstants.SYNC_PROXY_SERVICE_INFO_SNAPSHOT_ONLINE_COUNT,
                    VelocityCloudNetSyncProxyPlugin.getInstance().getProxyServer().getPlayerCount());
        }
    }

    @EventListener
    public void handle(CloudServiceInfoUpdateEvent event) {
        if (!event.getServiceInfo().getServiceId().getEnvironment().isMinecraftJavaProxy()) {
            return;
        }

        SyncProxyProxyLoginConfiguration syncProxyProxyLoginConfiguration = VelocityCloudNetSyncProxyPlugin.getInstance().getProxyLoginConfiguration();

        if (syncProxyProxyLoginConfiguration != null) {
            if (VelocityCloudNetSyncProxyPlugin.getInstance().inGroup(event.getServiceInfo(), syncProxyProxyLoginConfiguration) &&
                    event.getServiceInfo().getProperties().contains(SyncProxyConstants.SYNC_PROXY_SERVICE_INFO_SNAPSHOT_ONLINE_COUNT)) {
                VelocityCloudNetSyncProxyPlugin.getInstance().getOnlineCountOfProxies().put(event.getServiceInfo().getServiceId().getUniqueId(), event.getServiceInfo()
                        .getProperties().getInt(SyncProxyConstants.SYNC_PROXY_SERVICE_INFO_SNAPSHOT_ONLINE_COUNT));
            }
        }

        for (Player player : VelocityCloudNetSyncProxyPlugin.getInstance().getProxyServer().getAllPlayers()) {
            VelocityCloudNetSyncProxyPlugin.getInstance().setTabList(player);
        }
    }

    @EventListener
    public void handle(CloudServiceStopEvent event) {
        this.broadcastStartStop("service-stop", event.getServiceInfo());

        if (!event.getServiceInfo().getServiceId().getEnvironment().isMinecraftJavaProxy()) {
            return;
        }

        VelocityCloudNetSyncProxyPlugin.getInstance().getOnlineCountOfProxies().remove(event.getServiceInfo().getServiceId().getUniqueId());
    }

    @EventListener
    public void handle(CloudServiceStartEvent event) {
        this.broadcastStartStop("service-start", event.getServiceInfo());
    }

    private void broadcastStartStop(String key, ServiceInfoSnapshot serviceInfoSnapshot) {
        SyncProxyConfiguration configuration = SyncProxyConfigurationProvider.load();
        if (configuration != null && configuration.showIngameServicesStartStopMessages()) {
            String message = configuration.getMessages().get(key).replace("%service%", serviceInfoSnapshot.getServiceId().getName()).replace("&", "§");

            for (Player player : VelocityCloudNetSyncProxyPlugin.getInstance().getProxyServer().getAllPlayers()) {
                if (player.hasPermission("cloudnet.syncproxy.notify")) {
                    player.sendMessage(TextComponent.of(message));
                }
            }
        }
    }

    @EventListener
    public void handle(CloudServiceDisconnectNetworkEvent event) {
        if (!event.getServiceInfo().getServiceId().getEnvironment().isMinecraftJavaProxy()) {
            return;
        }

        VelocityCloudNetSyncProxyPlugin.getInstance().getOnlineCountOfProxies().remove(event.getServiceInfo().getServiceId().getUniqueId());
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
        SyncProxyProxyLoginConfiguration syncProxyProxyLoginConfiguration = VelocityCloudNetSyncProxyPlugin.getInstance().getProxyLoginConfiguration();

        if (syncProxyProxyLoginConfiguration != null) {
            for (Player player : VelocityCloudNetSyncProxyPlugin.getInstance().getProxyServer().getAllPlayers()) {
                if (
                        syncProxyProxyLoginConfiguration.isMaintenance() &&
                                syncProxyProxyLoginConfiguration.getWhitelist() != null &&
                                !syncProxyProxyLoginConfiguration.getWhitelist().contains(player.getUsername()) &&
                                !syncProxyProxyLoginConfiguration.getWhitelist().contains(player.getUniqueId().toString()) &&
                                !player.hasPermission("cloudnet.syncproxy.maintenance")) {
                    player.disconnect(TextComponent.of((SyncProxyConfigurationProvider.load().getMessages().get("player-login-not-whitelisted")).replace("&", "§")));
                }
            }
        }
    }
}