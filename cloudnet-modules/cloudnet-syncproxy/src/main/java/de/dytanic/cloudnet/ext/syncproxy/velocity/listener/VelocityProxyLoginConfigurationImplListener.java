package de.dytanic.cloudnet.ext.syncproxy.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyConfigurationProvider;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyMotd;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyProxyLoginConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.velocity.VelocityCloudNetSyncProxyPlugin;
import de.dytanic.cloudnet.wrapper.Wrapper;
import net.kyori.text.TextComponent;

import java.util.Collections;
import java.util.Random;
import java.util.UUID;

public final class VelocityProxyLoginConfigurationImplListener {

    @Subscribe
    public void handle(ProxyPingEvent event) {
        SyncProxyProxyLoginConfiguration syncProxyProxyLoginConfiguration = VelocityCloudNetSyncProxyPlugin.getInstance().getProxyLoginConfiguration();

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
                int onlinePlayers = VelocityCloudNetSyncProxyPlugin.getInstance().getSyncProxyOnlineCount();

                event.setPing(new ServerPing(
                        syncProxyMotd.getProtocolText() != null ? new ServerPing.Version(1,
                                syncProxyMotd.getProtocolText()
                                        .replace("%proxy%", Wrapper.getInstance().getServiceId().getName())
                                        .replace("%proxy_uniqueId%", String.valueOf(Wrapper.getInstance().getServiceId().getUniqueId()))
                                        .replace("%task%", Wrapper.getInstance().getServiceId().getTaskName())
                                        .replace("%node%", Wrapper.getInstance().getServiceId().getNodeUniqueId())
                                        .replace("%online_players%", String.valueOf(onlinePlayers))
                                        .replace("%max_players%", String.valueOf(syncProxyProxyLoginConfiguration.getMaxPlayers()))
                                        .replace("&", "§")) :
                                event.getPing().getVersion(),
                        new ServerPing.Players(
                                onlinePlayers,
                                syncProxyMotd.isAutoSlot() ?
                                        (syncProxyMotd.getAutoSlotMaxPlayersDistance() + onlinePlayers) :
                                        syncProxyProxyLoginConfiguration.getMaxPlayers(),
                                syncProxyMotd.getPlayerInfo() != null ?
                                        Iterables.map(
                                                syncProxyMotd.getPlayerInfo(),
                                                s -> new ServerPing.SamplePlayer(
                                                        s.replace("&", "§"),
                                                        UUID.randomUUID()
                                                )
                                        )
                                        :
                                        Collections.EMPTY_LIST
                        ),
                        TextComponent.of((syncProxyMotd.getFirstLine() + "\n" + syncProxyMotd.getSecondLine())
                                .replace("%proxy%", Wrapper.getInstance().getServiceId().getName())
                                .replace("%proxy_uniqueId%", String.valueOf(Wrapper.getInstance().getServiceId().getUniqueId()))
                                .replace("%task%", Wrapper.getInstance().getServiceId().getTaskName())
                                .replace("%node%", Wrapper.getInstance().getServiceId().getNodeUniqueId())
                                .replace("&", "§")),
                        event.getPing().getFavicon().isPresent() ? event.getPing().getFavicon().get() : null,
                        event.getPing().getModinfo().isPresent() ? event.getPing().getModinfo().get() : null
                ));
            }
        }
    }

    @Subscribe
    public void handle(LoginEvent event) {
        SyncProxyProxyLoginConfiguration syncProxyProxyLoginConfiguration = VelocityCloudNetSyncProxyPlugin.getInstance().getProxyLoginConfiguration();

        if (syncProxyProxyLoginConfiguration != null) {
            if (syncProxyProxyLoginConfiguration.isMaintenance() && syncProxyProxyLoginConfiguration.getWhitelist() != null) {
                if (syncProxyProxyLoginConfiguration.getWhitelist().contains(event.getPlayer().getUsername()) ||
                        syncProxyProxyLoginConfiguration.getWhitelist().contains(event.getPlayer().getUniqueId().toString()) ||
                        event.getPlayer().hasPermission("cloudnet.syncproxy.maintenance")) {
                    return;
                }

                event.setResult(LoginEvent.ComponentResult.denied(TextComponent.of((SyncProxyConfigurationProvider.load().getMessages()
                        .get("player-login-not-whitelisted")).replace("&", "§"))));
                return;
            }

            if (VelocityCloudNetSyncProxyPlugin.getInstance().getSyncProxyOnlineCount() >= syncProxyProxyLoginConfiguration.getMaxPlayers() &&
                    !event.getPlayer().hasPermission("cloudnet.syncproxy.fulljoin")) {
                event.setResult(LoginEvent.ComponentResult.denied(TextComponent.of(
                        SyncProxyConfigurationProvider.load().getMessages()
                                .getOrDefault("player-login-full-server", "&cThe network is currently full. You need extra permissions to enter the network").replace("&", "§")
                )));
            }
        }
    }
}