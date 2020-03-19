package de.dytanic.cloudnet.ext.bridge.velocity;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.PluginInfo;
import de.dytanic.cloudnet.ext.bridge.ProxyFallbackConfiguration;
import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkServiceInfo;
import de.dytanic.cloudnet.ext.bridge.velocity.event.VelocityPlayerFallbackEvent;
import de.dytanic.cloudnet.wrapper.Wrapper;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class VelocityCloudNetHelper {

    public static final Map<String, ServiceInfoSnapshot> SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION = new ConcurrentHashMap<>();

    private static ProxyServer proxyServer;

    private VelocityCloudNetHelper() {
        throw new UnsupportedOperationException();
    }

    public static void addServerToVelocityPrioritySystemConfiguration(ServiceInfoSnapshot serviceInfoSnapshot, String name) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(serviceInfoSnapshot);

        handleWithListenerInfoServerPriority(collection -> {
            for (ProxyFallbackConfiguration bungeeFallbackConfiguration : BridgeConfigurationProvider.load().getBungeeFallbackConfigurations()) {
                if (bungeeFallbackConfiguration != null && bungeeFallbackConfiguration.getFallbacks() != null &&
                        bungeeFallbackConfiguration.getTargetGroup() != null && Arrays.asList(Wrapper.getInstance().getCurrentServiceInfoSnapshot().getConfiguration().getGroups()).contains(bungeeFallbackConfiguration.getTargetGroup())) {
                    if (!collection.contains(name) && bungeeFallbackConfiguration.getDefaultFallbackTask().equals(serviceInfoSnapshot.getServiceId().getTaskName())) {
                        collection.add(name);
                    }
                }
            }
        });
    }

    public static void removeServerToVelocityPrioritySystemConfiguration(ServiceInfoSnapshot serviceInfoSnapshot, String name) {
        Preconditions.checkNotNull(name);

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
                        Wrapper.getInstance().getServiceId(),
                        Wrapper.getInstance().getCurrentServiceInfoSnapshot().getConfiguration().getGroups()
                )
        );
    }

    public static String filterServiceForPlayer(Player player, String currentServer) {
        ServiceInfoSnapshot fallback = BridgeHelper.filterServiceForPlayer(
                currentServer,
                VelocityCloudNetHelper::getFilteredEntries,
                player::hasPermission
        );
        try {
            return proxyServer.getEventManager().fire(new VelocityPlayerFallbackEvent(player,
                    fallback, fallback != null ? fallback.getServiceId().getName() : null)
            ).get().getFallbackName();
        } catch (InterruptedException | ExecutionException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    public static boolean isServiceEnvironmentTypeProvidedForVelocity(ServiceInfoSnapshot serviceInfoSnapshot) {
        Preconditions.checkNotNull(serviceInfoSnapshot);
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

        return BridgeHelper.isFallbackService(serviceInfoSnapshot);
    }

    private static List<Map.Entry<String, ServiceInfoSnapshot>> getFilteredEntries(String task, String currentServer) {
        return SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION.entrySet().stream().filter(stringServiceInfoSnapshotEntry -> {
            if (currentServer != null && currentServer.equalsIgnoreCase(stringServiceInfoSnapshotEntry.getKey())) {
                return false;
            }

            return task.equals(stringServiceInfoSnapshotEntry.getValue().getServiceId().getTaskName());
        }).collect(Collectors.toList());
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
                .append("Players", proxyServer.getAllPlayers().stream().map(player -> new VelocityCloudNetPlayerInfo(
                        player.getUniqueId(),
                        player.getUsername(),
                        player.getCurrentServer().isPresent() ? player.getCurrentServer().get().getServerInfo().getName() : null,
                        (int) player.getPing(),
                        new HostAndPort(player.getRemoteAddress())
                )).collect(Collectors.toList()))
                .append("Plugins", proxyServer.getPluginManager().getPlugins().stream().map(pluginContainer -> {
                    PluginInfo pluginInfo = new PluginInfo(
                            pluginContainer.getDescription().getName().orElse(null),
                            pluginContainer.getDescription().getVersion().orElse(null)
                    );

                    pluginInfo.getProperties()
                            .append("authors", pluginContainer.getDescription().getAuthors())
                            .append("depends", pluginContainer.getDescription().getDependencies())
                    ;

                    return pluginInfo;
                }).collect(Collectors.toList()))
        ;
    }

    public static ProxyServer getProxyServer() {
        return VelocityCloudNetHelper.proxyServer;
    }

    public static void setProxyServer(ProxyServer proxyServer) {
        VelocityCloudNetHelper.proxyServer = proxyServer;
    }
}