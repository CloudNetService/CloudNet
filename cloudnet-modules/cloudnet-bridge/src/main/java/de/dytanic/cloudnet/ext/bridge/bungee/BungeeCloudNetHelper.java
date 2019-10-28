package de.dytanic.cloudnet.ext.bridge.bungee;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.collection.Maps;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.ext.bridge.*;
import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkServiceInfo;
import de.dytanic.cloudnet.wrapper.Wrapper;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

public final class BungeeCloudNetHelper {

    public static final Map<String, ServiceInfoSnapshot> SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION = Maps.newConcurrentHashMap();

    private BungeeCloudNetHelper() {
        throw new UnsupportedOperationException();
    }


    public static boolean isOnAFallbackInstance(ProxiedPlayer proxiedPlayer) {
        return proxiedPlayer.getServer() != null && isFallbackServer(proxiedPlayer.getServer().getInfo());
    }

    public static boolean isFallbackServer(ServerInfo serverInfo) {
        if (serverInfo == null) {
            return false;
        }
        ServiceInfoSnapshot serviceInfoSnapshot = SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION.get(serverInfo.getName());
        if (serviceInfoSnapshot == null) {
            return false;
        }

        return ProxyCloudNetHelper.isFallbackService(serviceInfoSnapshot);
    }

    public static String filterServiceForProxiedPlayer(ProxiedPlayer proxiedPlayer, String currentServer) {
        return ProxyCloudNetHelper.filterServiceForPlayer(
                currentServer,
                BungeeCloudNetHelper::getFilteredEntries,
                proxiedPlayer::hasPermission
        );
    }

    private static List<Map.Entry<String, ServiceInfoSnapshot>> getFilteredEntries(String task, String currentServer) {
        return Iterables.filter(
                SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION.entrySet(), stringServiceInfoSnapshotEntry -> {
                    if (stringServiceInfoSnapshotEntry.getValue().getLifeCycle() != ServiceLifeCycle.RUNNING
                            || (currentServer != null && currentServer.equalsIgnoreCase(stringServiceInfoSnapshotEntry.getKey()))) {
                        return false;
                    }

                    return task.equals(stringServiceInfoSnapshotEntry.getValue().getServiceId().getTaskName());
                });
    }

    public static boolean isServiceEnvironmentTypeProvidedForBungeeCord(ServiceInfoSnapshot serviceInfoSnapshot) {
        Validate.checkNotNull(serviceInfoSnapshot);
        ServiceEnvironmentType currentServiceEnvironment = Wrapper.getInstance().getCurrentServiceInfoSnapshot().getServiceId().getEnvironment();
        return (serviceInfoSnapshot.getServiceId().getEnvironment().isMinecraftJavaServer() && currentServiceEnvironment.isMinecraftJavaProxy())
                || (serviceInfoSnapshot.getServiceId().getEnvironment().isMinecraftBedrockServer() && currentServiceEnvironment.isMinecraftBedrockProxy());
    }

    public static void initProperties(ServiceInfoSnapshot serviceInfoSnapshot) {
        Validate.checkNotNull(serviceInfoSnapshot);

        serviceInfoSnapshot.getProperties()
                .append("Online", true)
                .append("Version", ProxyServer.getInstance().getVersion())
                .append("Game-Version", ProxyServer.getInstance().getGameVersion())
                .append("Online-Count", ProxyServer.getInstance().getOnlineCount())
                .append("Channels", ProxyServer.getInstance().getChannels())
                .append("BungeeCord-Name", ProxyServer.getInstance().getName())
                .append("Players", Iterables.map(ProxyServer.getInstance().getPlayers(), proxiedPlayer -> new BungeeCloudNetPlayerInfo(
                        proxiedPlayer.getUniqueId(),
                        proxiedPlayer.getName(),
                        proxiedPlayer.getServer() != null ? proxiedPlayer.getServer().getInfo().getName() : null,
                        proxiedPlayer.getPing(),
                        new HostAndPort(proxiedPlayer.getPendingConnection().getAddress())
                )))
                .append("Plugins", Iterables.map(ProxyServer.getInstance().getPluginManager().getPlugins(), plugin -> {
                    PluginInfo pluginInfo = new PluginInfo(plugin.getDescription().getName(), plugin.getDescription().getVersion());

                    pluginInfo.getProperties()
                            .append("author", plugin.getDescription().getAuthor())
                            .append("main-class", plugin.getDescription().getMain())
                            .append("depends", plugin.getDescription().getDepends())
                    ;

                    return pluginInfo;
                }))
        ;
    }

    public static NetworkConnectionInfo createNetworkConnectionInfo(PendingConnection pendingConnection) {
        return BridgeHelper.createNetworkConnectionInfo(
                pendingConnection.getUniqueId(),
                pendingConnection.getName(),
                pendingConnection.getVersion(),
                new HostAndPort(pendingConnection.getAddress()),
                new HostAndPort(pendingConnection.getListener().getHost()),
                pendingConnection.isOnlineMode(),
                pendingConnection.isLegacy(),
                new NetworkServiceInfo(
                        ServiceEnvironmentType.BUNGEECORD,
                        Wrapper.getInstance().getServiceId().getUniqueId(),
                        Wrapper.getInstance().getServiceId().getName()
                )
        );
    }


    public static ServerInfo createServerInfo(String name, InetSocketAddress address) {
        Validate.checkNotNull(name);
        Validate.checkNotNull(address);

        // with rakNet enabled to support bedrock servers on Waterdog
        if (Wrapper.getInstance().getCurrentServiceInfoSnapshot().getServiceId().getEnvironment() == ServiceEnvironmentType.WATERDOG) {
            try {
                Class<ProxyServer> proxyServerClass = ProxyServer.class;

                Method method = proxyServerClass.getMethod("constructServerInfo",
                        String.class, InetSocketAddress.class, String.class, boolean.class, boolean.class, String.class);
                method.setAccessible(true);
                return (ServerInfo) method.invoke(ProxyServer.getInstance(), name, address, "CloudNet provided serverInfo", false, true, "default");
            } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException exception) {
                Wrapper.getInstance().getLogger().log(LogLevel.ERROR, "Unable to enable rakNet, although using Waterdog: ", exception);
            }
        }

        return ProxyServer.getInstance().constructServerInfo(name, address, "CloudNet provided serverInfo", false);
    }
}
