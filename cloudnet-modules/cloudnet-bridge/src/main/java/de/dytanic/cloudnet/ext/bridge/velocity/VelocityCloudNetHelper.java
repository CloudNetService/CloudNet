package de.dytanic.cloudnet.ext.bridge.velocity;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.collection.Maps;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import de.dytanic.cloudnet.ext.bridge.PluginInfo;
import de.dytanic.cloudnet.ext.bridge.ProxyFallback;
import de.dytanic.cloudnet.ext.bridge.ProxyFallbackConfiguration;
import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkServiceInfo;
import de.dytanic.cloudnet.wrapper.Wrapper;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class VelocityCloudNetHelper {

    public static final Map<String, ServiceInfoSnapshot> SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION = Maps.newConcurrentHashMap();

    private static ProxyServer proxyServer;

    private VelocityCloudNetHelper() {
        throw new UnsupportedOperationException();
    }

    public static void addServerToVelocityPrioritySystemConfiguration(ServiceInfoSnapshot serviceInfoSnapshot, String name) {
        Validate.checkNotNull(name);
        Validate.checkNotNull(serviceInfoSnapshot);

        handleWithListenerInfoServerPriority(new Consumer<Collection<String>>() {
            @Override
            public void accept(Collection<String> collection) {
                for (ProxyFallbackConfiguration bungeeFallbackConfiguration : BridgeConfigurationProvider.load().getBungeeFallbackConfigurations())
                    if (bungeeFallbackConfiguration != null && bungeeFallbackConfiguration.getFallbacks() != null &&
                            bungeeFallbackConfiguration.getTargetGroup() != null && Iterables.contains(bungeeFallbackConfiguration.getTargetGroup(),
                            Wrapper.getInstance().getCurrentServiceInfoSnapshot().getConfiguration().getGroups()))
                        if (!collection.contains(name) && bungeeFallbackConfiguration.getDefaultFallbackTask().equals(serviceInfoSnapshot.getServiceId().getTaskName()))
                            collection.add(name);
            }
        });
    }

    public static void removeServerToVelocityPrioritySystemConfiguration(ServiceInfoSnapshot serviceInfoSnapshot, String name) {
        Validate.checkNotNull(name);

        handleWithListenerInfoServerPriority(new Consumer<Collection<String>>() {
            @Override
            public void accept(Collection<String> collection) {
                collection.remove(name);
            }
        });
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
        for (ProxyFallbackConfiguration proxyFallbackConfiguration : BridgeConfigurationProvider.load().getBungeeFallbackConfigurations())
            if (proxyFallbackConfiguration.getTargetGroup() != null && Iterables.contains(
                    proxyFallbackConfiguration.getTargetGroup(),
                    Wrapper.getInstance().getCurrentServiceInfoSnapshot().getConfiguration().getGroups()
            )) {
                List<ProxyFallback> proxyFallbacks = Iterables.newArrayList(proxyFallbackConfiguration.getFallbacks());
                Collections.sort(proxyFallbacks);

                String server = null;

                for (ProxyFallback proxyFallback : proxyFallbacks) {
                    if (proxyFallback.getTask() != null) continue;
                    if (proxyFallback.getPermission() != null && !player.hasPermission(proxyFallback.getPermission()))
                        continue;

                    List<Map.Entry<String, ServiceInfoSnapshot>> entries = getFilteredEntries(proxyFallback.getTask(), currentServer);

                    if (entries.size() == 0) continue;

                    server = entries.get(new Random().nextInt(entries.size())).getKey();
                }

                if (server == null) {
                    List<Map.Entry<String, ServiceInfoSnapshot>> entries = getFilteredEntries(proxyFallbackConfiguration.getDefaultFallbackTask(), currentServer);

                    if (entries.size() > 0)
                        server = entries.get(new Random().nextInt(entries.size())).getKey();
                }

                return server;
            }

        return null;
    }

    public static boolean isServiceEnvironmentTypeProvidedForVelocity(ServiceInfoSnapshot serviceInfoSnapshot) {
        Validate.checkNotNull(serviceInfoSnapshot);
        return serviceInfoSnapshot.getServiceId().getEnvironment().isMinecraftJavaServer();
    }

    public static boolean isOnAFallbackInstance(Player player) {
        ServiceInfoSnapshot serviceInfoSnapshot = SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION.get(player.getCurrentServer().get().getServerInfo().getName());

        for (ProxyFallbackConfiguration bungeeFallbackConfiguration : BridgeConfigurationProvider.load().getBungeeFallbackConfigurations())
            if (bungeeFallbackConfiguration.getTargetGroup() != null && Iterables.contains(
                    bungeeFallbackConfiguration.getTargetGroup(),
                    Wrapper.getInstance().getCurrentServiceInfoSnapshot().getConfiguration().getGroups()
            )) {
                for (ProxyFallback bungeeFallback : bungeeFallbackConfiguration.getFallbacks())
                    if (bungeeFallback.getTask() != null && serviceInfoSnapshot.getServiceId().getTaskName().equals(bungeeFallback.getTask()))
                        return true;
            }

        return false;
    }

    private static List<Map.Entry<String, ServiceInfoSnapshot>> getFilteredEntries(String task, String currentServer) {
        return Iterables.filter(
                SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION.entrySet(), new Predicate<Map.Entry<String, ServiceInfoSnapshot>>() {

                    @Override
                    public boolean test(Map.Entry<String, ServiceInfoSnapshot> stringServiceInfoSnapshotEntry) {
                        if (currentServer != null && currentServer.equalsIgnoreCase(stringServiceInfoSnapshotEntry.getKey()))
                            return false;

                        return task.equals(stringServiceInfoSnapshotEntry.getValue().getServiceId().getTaskName());
                    }
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
                .append("Players", Iterables.map(proxyServer.getAllPlayers(), new Function<Player, VelocityCloudNetPlayerInfo>() {
                    @Override
                    public VelocityCloudNetPlayerInfo apply(Player player) {
                        return new VelocityCloudNetPlayerInfo(
                                player.getUniqueId(),
                                player.getUsername(),
                                player.getCurrentServer().isPresent() ? player.getCurrentServer().get().getServerInfo().getName() : null,
                                (int) player.getPing(),
                                new HostAndPort(player.getRemoteAddress())
                        );
                    }
                }))
                .append("Plugins", Iterables.map(proxyServer.getPluginManager().getPlugins(), new Function<PluginContainer, PluginInfo>() {
                    @Override
                    public PluginInfo apply(PluginContainer pluginContainer) {
                        PluginInfo pluginInfo = new PluginInfo(
                                pluginContainer.getDescription().getName().get(),
                                pluginContainer.getDescription().getVersion().get()
                        );

                        pluginInfo.getProperties()
                                .append("authors", pluginContainer.getDescription().getAuthors())
                                .append("depends", pluginContainer.getDescription().getDependencies())
                        ;

                        return pluginInfo;
                    }
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