package de.dytanic.cloudnet.ext.syncproxy.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import de.dytanic.cloudnet.ext.syncproxy.AbstractSyncProxyManagement;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyMotd;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyProxyLoginConfiguration;
import de.dytanic.cloudnet.wrapper.Wrapper;
import net.kyori.text.TextComponent;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

// TODO: Add TabList update on ServerConnect
public final class VelocityProxyLoginConfigurationImplListener {

    private AbstractSyncProxyManagement syncProxyManagement;

    public VelocityProxyLoginConfigurationImplListener(AbstractSyncProxyManagement syncProxyManagement) {
        this.syncProxyManagement = syncProxyManagement;
    }

    @Subscribe
    public void handle(ProxyPingEvent event) {
        SyncProxyProxyLoginConfiguration syncProxyProxyLoginConfiguration = this.syncProxyManagement.getLoginConfiguration();

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
                int onlinePlayers = this.syncProxyManagement.getSyncProxyOnlineCount();

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
                                        Arrays.stream(syncProxyMotd.getPlayerInfo())
                                                .map(s -> new ServerPing.SamplePlayer(
                                                        s.replace("&", "§"),
                                                        UUID.randomUUID()
                                                )).collect(Collectors.toList())
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
        SyncProxyProxyLoginConfiguration syncProxyProxyLoginConfiguration = this.syncProxyManagement.getLoginConfiguration();

        if (syncProxyProxyLoginConfiguration != null) {
            if (syncProxyProxyLoginConfiguration.isMaintenance() && syncProxyProxyLoginConfiguration.getWhitelist() != null) {
                if (syncProxyProxyLoginConfiguration.getWhitelist().contains(event.getPlayer().getUsername()) ||
                        syncProxyProxyLoginConfiguration.getWhitelist().contains(event.getPlayer().getUniqueId().toString()) ||
                        event.getPlayer().hasPermission("cloudnet.syncproxy.maintenance")) {
                    return;
                }

                event.setResult(LoginEvent.ComponentResult.denied(TextComponent.of((this.syncProxyManagement.getSyncProxyConfiguration().getMessages()
                        .get("player-login-not-whitelisted")).replace("&", "§"))));
                return;
            }

            if (this.syncProxyManagement.getSyncProxyOnlineCount() >= syncProxyProxyLoginConfiguration.getMaxPlayers() &&
                    !event.getPlayer().hasPermission("cloudnet.syncproxy.fulljoin")) {
                event.setResult(LoginEvent.ComponentResult.denied(TextComponent.of(
                        this.syncProxyManagement.getSyncProxyConfiguration().getMessages()
                                .getOrDefault("player-login-full-server", "&cThe network is currently full. You need extra permissions to enter the network").replace("&", "§")
                )));
            }
        }
    }

}