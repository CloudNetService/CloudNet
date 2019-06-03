package de.dytanic.cloudnet.ext.syncproxy.bungee.listener;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyConfigurationProvider;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyMotd;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyProxyLoginConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.bungee.BungeeCloudNetSyncProxyPlugin;
import de.dytanic.cloudnet.ext.syncproxy.bungee.util.LoginPendingConnectionCommandSender;
import de.dytanic.cloudnet.wrapper.Wrapper;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.UUID;

public final class BungeeProxyLoginConfigurationImplListener implements Listener {

    @EventHandler
    public void handle(ProxyPingEvent event) {
        SyncProxyProxyLoginConfiguration syncProxyProxyLoginConfiguration = BungeeCloudNetSyncProxyPlugin.getInstance().getProxyLoginConfiguration();

        if (syncProxyProxyLoginConfiguration != null) {
            SyncProxyMotd syncProxyMotd = null;
            Random random = new Random();

            if (syncProxyProxyLoginConfiguration.isMaintenance()) {
                if (syncProxyProxyLoginConfiguration.getMaintenanceMotds() != null && !syncProxyProxyLoginConfiguration.getMaintenanceMotds().isEmpty())
                    syncProxyMotd = syncProxyProxyLoginConfiguration.getMaintenanceMotds().get(random.nextInt(
                            syncProxyProxyLoginConfiguration.getMaintenanceMotds().size()));
            } else {
                if (syncProxyProxyLoginConfiguration.getMotds() != null && !syncProxyProxyLoginConfiguration.getMotds().isEmpty())
                    syncProxyMotd = syncProxyProxyLoginConfiguration.getMotds().get(random.nextInt(
                            syncProxyProxyLoginConfiguration.getMotds().size()));
            }

            if (syncProxyMotd != null) {
                int onlinePlayers = BungeeCloudNetSyncProxyPlugin.getInstance().getSyncProxyOnlineCount();

                ServerPing serverPing = createServerPingInstance(
                        event,
                        ChatColor.translateAlternateColorCodes('&', syncProxyMotd.getFirstLine() + "\n" + syncProxyMotd.getSecondLine() + "")
                                .replace("%proxy%", Wrapper.getInstance().getServiceId().getName() + "")
                                .replace("%proxy_uniqueId%", Wrapper.getInstance().getServiceId().getUniqueId() + "")
                                .replace("%task%", Wrapper.getInstance().getServiceId().getTaskName() + "")
                                .replace("%node%", Wrapper.getInstance().getServiceId().getNodeUniqueId() + ""),
                        syncProxyMotd.getProtocolText(),
                        onlinePlayers,
                        syncProxyMotd.isAutoSlot() ?
                                (syncProxyMotd.getAutoSlotMaxPlayersDistance() + onlinePlayers) :
                                syncProxyProxyLoginConfiguration.getMaxPlayers(),
                        syncProxyMotd.getPlayerInfo()
                );

                if (serverPing != null) {
                    event.setResponse(serverPing);
                }
            }
        }
    }

    @EventHandler
    public void handle(LoginEvent event) {
        SyncProxyProxyLoginConfiguration syncProxyProxyLoginConfiguration = BungeeCloudNetSyncProxyPlugin.getInstance().getProxyLoginConfiguration();

        if (syncProxyProxyLoginConfiguration != null) {
            LoginPendingConnectionCommandSender loginEventCommandSender = new LoginPendingConnectionCommandSender(event, getUniqueIdOfPendingConnection(event.getConnection()));

            if (syncProxyProxyLoginConfiguration.isMaintenance() && syncProxyProxyLoginConfiguration.getWhitelist() != null) {
                if (syncProxyProxyLoginConfiguration.getWhitelist().contains(event.getConnection().getName()))
                    return;

                UUID uniqueId = getUniqueIdOfPendingConnection(event.getConnection());

                if ((uniqueId != null && syncProxyProxyLoginConfiguration.getWhitelist().contains(uniqueId.toString())) ||
                        loginEventCommandSender.hasPermission("cloudnet.syncproxy.maintenance"))
                    return;

                event.setCancelled(true);
                event.setCancelReason(ChatColor.translateAlternateColorCodes('&',
                        SyncProxyConfigurationProvider.load().getMessages().get("player-login-not-whitelisted") + ""
                ));
                return;
            }

            if (BungeeCloudNetSyncProxyPlugin.getInstance().getSyncProxyOnlineCount() >= syncProxyProxyLoginConfiguration.getMaxPlayers() &&
                    !loginEventCommandSender.hasPermission("cloudnet.syncproxy.fullljoin")) {
                event.setCancelled(true);
                event.setCancelReason(ChatColor.translateAlternateColorCodes('&', SyncProxyConfigurationProvider.load().getMessages()
                        .getOrDefault("player-login-full-server", "&cThe network is currently full. You need extra permissions to enter the network") + ""));
            }
        }
    }

    private ServerPing createServerPingInstance(ProxyPingEvent event,
                                                String motd,
                                                String protocolText, int onlinePlayers, int maxPlayers, String[] playerInfo) {
        Validate.checkNotNull(event);
        Validate.checkNotNull(motd);

        //up to 1.7
        try {
            Class<?> protocolClass = Class.forName("net.md_5.bungee.api.ServerPing$Protocol");
            Class<?> playersClass = Class.forName("net.md_5.bungee.api.ServerPing$Players");
            Class<?> playerInfoClass = Class.forName("net.md_5.bungee.api.ServerPing$PlayerInfo");
            //Class<?> baseComponentClass = Class.forName("net.md_5.bungee.api.chat.BaseComponent");
            Class<?> faviconClass = Class.forName("net.md_5.bungee.api.Favicon");
            Class<?> textComponentClass = Class.forName("net.md_5.bungee.api.chat.TextComponent");

            Constructor<?> playerInfoClassConstructor = playerInfoClass.getConstructor(String.class, String.class);

            Object array = Array.newInstance(playerInfoClass, playerInfo != null ? playerInfo.length : 0);

            if (playerInfo != null)
                for (int i = 0; i < playerInfo.length; i++)
                    Array.set(array, i, playerInfoClassConstructor.newInstance(
                            ChatColor.translateAlternateColorCodes('&', playerInfo[i]), UUID.randomUUID().toString()));

            Method methodFromLegacyTest = textComponentClass.getMethod("fromLegacyText", String.class);
            methodFromLegacyTest.setAccessible(true);

            Method methodGetFaviconObject = ServerPing.class.getDeclaredMethod("getFaviconObject");
            methodGetFaviconObject.setAccessible(true);

            Method methodGetVersion = ServerPing.class.getDeclaredMethod("getVersion");
            methodGetVersion.setAccessible(true);
            Object protocol = methodGetVersion.invoke(event.getResponse());

            Object favicon = methodGetFaviconObject.invoke(event.getResponse());

            return ServerPing.class.getConstructor(protocolClass, playersClass, String.class, faviconClass)
                    .newInstance(
                            protocolText == null && protocol != null ? protocol : protocolClass.getConstructor(String.class, int.class).newInstance(
                                    ChatColor.translateAlternateColorCodes('&',
                                            (protocolText == null ? ProxyServer.getInstance().getName() + " " + ProxyServer.getInstance().getGameVersion() : protocolText)
                                                    .replace("%proxy%", Wrapper.getInstance().getServiceId().getName() + "")
                                                    .replace("%proxy_uniqueId%", Wrapper.getInstance().getServiceId().getUniqueId() + "")
                                                    .replace("%task%", Wrapper.getInstance().getServiceId().getTaskName() + "")
                                                    .replace("%node%", Wrapper.getInstance().getServiceId().getNodeUniqueId() + "")
                                                    .replace("%online_players%", onlinePlayers + "")
                                                    .replace("%max_players%", maxPlayers + "")
                                                    + ""),
                                    (protocolText == null ? ProxyServer.getInstance().getProtocolVersion() : 1)),
                            //supports all BungeeCord versions
                            playersClass.getConstructor(int.class, int.class, array.getClass()).newInstance(maxPlayers, onlinePlayers, array),
                            //supports only newer BungeeCord version up to MC 1.9
                            //textComponentClass.getConstructor(Array.newInstance(baseComponentClass, 1).getClass()).newInstance(methodFromLegacyTest.invoke(null, motd)),
                            motd,
                            favicon
                    );
        } catch (Exception ignored) {
        }

        //old
        try {

            Method method = ServerPing.class.getMethod("getProtocolVersion");
            method.setAccessible(true);

            Constructor<ServerPing> constructor = ServerPing.class.getConstructor(byte.class, String.class, String.class, int.class, int.class);

            return constructor.newInstance(method.invoke(event.getResponse()), ProxyServer.getInstance().getGameVersion(), motd, onlinePlayers, maxPlayers);
        } catch (Exception ignored) {
        }

        return null;
    }

    private UUID getUniqueIdOfPendingConnection(PendingConnection pendingConnection) {
        try {

            Method method = PendingConnection.class.getMethod("getUniqueId");
            method.setAccessible(true);

            return (UUID) method.invoke(pendingConnection);

        } catch (Exception ignored) {
        }

        return null;
    }
}