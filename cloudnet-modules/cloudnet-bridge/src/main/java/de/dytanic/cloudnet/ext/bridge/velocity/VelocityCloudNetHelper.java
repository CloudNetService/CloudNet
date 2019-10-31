package de.dytanic.cloudnet.ext.bridge.velocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.collection.Maps;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import de.dytanic.cloudnet.ext.bridge.PluginInfo;
import de.dytanic.cloudnet.ext.bridge.ProxyCloudNetHelper;
import de.dytanic.cloudnet.ext.bridge.ProxyFallbackConfiguration;
import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkServiceInfo;
import de.dytanic.cloudnet.wrapper.Wrapper;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class VelocityCloudNetHelper {

    public static final Map<String, ServiceInfoSnapshot> SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION = Maps.newConcurrentHashMap();

    private static ProxyServer proxyServer;

    private VelocityCloudNetHelper() {
        throw new UnsupportedOperationException();
    }

    public static void addServerToVelocityPrioritySystemConfiguration(ServiceInfoSnapshot serviceInfoSnapshot, String name) {
        Validate.checkNotNull(name);
        Validate.checkNotNull(serviceInfoSnapshot);

        handleWithListenerInfoServerPriority(collection -> {
            for (ProxyFallbackConfiguration bungeeFallbackConfiguration : BridgeConfigurationProvider.load().getBungeeFallbackConfigurations()) {
                if (bungeeFallbackConfiguration != null && bungeeFallbackConfiguration.getFallbacks() != null &&
                        bungeeFallbackConfiguration.getTargetGroup() != null && Iterables.contains(bungeeFallbackConfiguration.getTargetGroup(),
                        Wrapper.getInstance().getCurrentServiceInfoSnapshot().getConfiguration().getGroups())) {
                    if (!collection.contains(name) && bungeeFallbackConfiguration.getDefaultFallbackTask().equals(serviceInfoSnapshot.getServiceId().getTaskName())) {
                        collection.add(name);
                    }
                }
            }
        });
    }

    public static void removeServerToVelocityPrioritySystemConfiguration(ServiceInfoSnapshot serviceInfoSnapshot, String name) {
        Validate.checkNotNull(name);

        handleWithListenerInfoServerPriority(collection -> collection.remove(name));
    }

    public static void handleWithListenerInfoServerPriority(Consumer<Collection<String>> listenerInfoConsumer) {
        listenerInfoConsumer.accept(proxyServer.getConfiguration().getAttemptConnectionOrder());
    }

    public static void updateServiceInfo() {
        Wrapper.getInstance().publishServiceInfoUpdate();
    }

    public static NetworkConnectionInfo createNetworkConnectionInfo(Player player) {
        return new NetworkConnectionInfo(
                player.getUniqueId(),
                player.getUsername(),
                player.getProtocolVersion().getProtocol(),
                new HostAndPort(player.getRemoteAddress()),
                new HostAndPort(proxyServer.getBoundAddress()),
                proxyServer.getConfiguration().isOnlineMode(),
                true,
                new NetworkServiceInfo(
                        ServiceEnvironmentType.VELOCITY,
                        Wrapper.getInstance().getServiceId().getUniqueId(),
                        Wrapper.getInstance().getServiceId().getName()
                )
        );
    }

    public static String filterServiceForPlayer(Player player, String currentServer) {
        return ProxyCloudNetHelper.filterServiceForPlayer(
                currentServer,
                VelocityCloudNetHelper::getFilteredEntries,
                player::hasPermission
        );
    }

    public static boolean isServiceEnvironmentTypeProvidedForVelocity(ServiceInfoSnapshot serviceInfoSnapshot) {
        Validate.checkNotNull(serviceInfoSnapshot);
        return serviceInfoSnapshot.getServiceId().getEnvironment().isMinecraftJavaServer();
    }

    public static boolean isOnAFallbackInstance(Player player) {
        return player.getCurrentServer().isPresent() && isFallbackServer(player.getCurrentServer().get().getServerInfo());
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

    private static List<Map.Entry<String, ServiceInfoSnapshot>> getFilteredEntries(String task, String currentServer) {
        return Iterables.filter(
                SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION.entrySet(), stringServiceInfoSnapshotEntry -> {
                    if (currentServer != null && currentServer.equalsIgnoreCase(stringServiceInfoSnapshotEntry.getKey())) {
                        return false;
                    }

                    return task.equals(stringServiceInfoSnapshotEntry.getValue().getServiceId().getTaskName());
                });
    }

    public static void initProperties(ServiceInfoSnapshot serviceInfoSnapshot) {
        serviceInfoSnapshot.getProperties()
                .append("Online", true)
                .append("Version", proxyServer.getVersion().getVersion())
                .append("Version-Vendor", proxyServer.getVersion().getVendor())
                .append("Velocity-Name", proxyServer.getVersion().getName())
                .append("Online-Count", proxyServer.getPlayerCount())
                .append("Online-Mode", proxyServer.getConfiguration().isOnlineMode())
                .append("Compression-Level", proxyServer.getConfiguration().getCompressionLevel())
                .append("Connection-Timeout", proxyServer.getConfiguration().getConnectTimeout())
                .append("Players", Iterables.map(proxyServer.getAllPlayers(), player -> new VelocityCloudNetPlayerInfo(
                        player.getUniqueId(),
                        player.getUsername(),
                        player.getCurrentServer().isPresent() ? player.getCurrentServer().get().getServerInfo().getName() : null,
                        (int) player.getPing(),
                        new HostAndPort(player.getRemoteAddress())
                )))
                .append("Plugins", Iterables.map(proxyServer.getPluginManager().getPlugins(), pluginContainer -> {
                    PluginInfo pluginInfo = new PluginInfo(
                            pluginContainer.getDescription().getName().orElse(null),
                            pluginContainer.getDescription().getVersion().orElse(null)
                    );

                    pluginInfo.getProperties()
                            .append("authors", pluginContainer.getDescription().getAuthors())
                            .append("depends", pluginContainer.getDescription().getDependencies())
                    ;

                    return pluginInfo;
                }))
        ;
    }

    public static ProxyServer getProxyServer() {
        return VelocityCloudNetHelper.proxyServer;
    }

    public static void setProxyServer(ProxyServer proxyServer) {
        VelocityCloudNetHelper.proxyServer = proxyServer;
    }
}