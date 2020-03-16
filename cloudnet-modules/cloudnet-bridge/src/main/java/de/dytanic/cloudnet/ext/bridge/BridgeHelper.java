package de.dytanic.cloudnet.ext.bridge;

import de.dytanic.cloudnet.common.annotation.UnsafeClass;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.player.ICloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkPlayerServerInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkServiceInfo;
import de.dytanic.cloudnet.wrapper.Wrapper;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

@UnsafeClass
public final class BridgeHelper {

    private BridgeHelper() {
        throw new UnsupportedOperationException();
    }

    public static void updateServiceInfo() {
        Wrapper.getInstance().publishServiceInfoUpdate();
    }

    public static void sendChannelMessageProxyLoginRequest(NetworkConnectionInfo networkConnectionInfo) {
        CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL,
                BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_LOGIN_REQUEST,
                new JsonDocument("networkConnectionInfo", networkConnectionInfo)
        );
    }

    public static void sendChannelMessageProxyLoginSuccess(NetworkConnectionInfo networkConnectionInfo) {
        CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL,
                BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_LOGIN_SUCCESS,
                new JsonDocument("networkConnectionInfo", networkConnectionInfo)
        );
    }

    public static void sendChannelMessageProxyDisconnect(NetworkConnectionInfo networkConnectionInfo) {
        CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL,
                BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_DISCONNECT,
                new JsonDocument("networkConnectionInfo", networkConnectionInfo)
        );
    }

    public static void sendChannelMessageProxyServerSwitch(NetworkConnectionInfo networkConnectionInfo, NetworkServiceInfo networkServiceInfo) {
        CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL,
                BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_SERVER_SWITCH,
                new JsonDocument("networkConnectionInfo", networkConnectionInfo)
                        .append("networkServiceInfo", networkServiceInfo)
        );
    }

    public static void sendChannelMessageProxyServerConnectRequest(NetworkConnectionInfo networkConnectionInfo, NetworkServiceInfo networkServiceInfo) {
        CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL,
                BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_SERVER_CONNECT_REQUEST,
                new JsonDocument("networkConnectionInfo", networkConnectionInfo)
                        .append("networkServiceInfo", networkServiceInfo)
        );
    }

    public static void sendChannelMessageServerLoginRequest(NetworkConnectionInfo networkConnectionInfo, NetworkPlayerServerInfo networkPlayerServerInfo) {
        CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL,
                BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_SERVER_LOGIN_REQUEST,
                new JsonDocument("networkConnectionInfo", networkConnectionInfo)
                        .append("networkPlayerServerInfo", networkPlayerServerInfo)
        );
    }

    public static void sendChannelMessageServerLoginSuccess(NetworkConnectionInfo networkConnectionInfo, NetworkPlayerServerInfo networkPlayerServerInfo) {
        CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL,
                BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_SERVER_LOGIN_SUCCESS,
                new JsonDocument("networkConnectionInfo", networkConnectionInfo)
                        .append("networkPlayerServerInfo", networkPlayerServerInfo)
        );
    }

    public static void sendChannelMessageServerDisconnect(NetworkConnectionInfo networkConnectionInfo, NetworkPlayerServerInfo networkPlayerServerInfo) {
        CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL,
                BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_SERVER_DISCONNECT,
                new JsonDocument("networkConnectionInfo", networkConnectionInfo)
                        .append("networkPlayerServerInfo", networkPlayerServerInfo)
        );
    }

    public static NetworkConnectionInfo createNetworkConnectionInfo(
            UUID uniqueId,
            String name,
            int version,
            HostAndPort userAddress,
            HostAndPort listener,
            boolean onlineMode,
            boolean legacy,
            NetworkServiceInfo networkServiceInfo
    ) {
        return new NetworkConnectionInfo(uniqueId, name, version, userAddress, listener, onlineMode, legacy, networkServiceInfo);
    }

    public static boolean playerIsOnProxy(UUID uuid, String playerAddress) {
        ICloudPlayer cloudPlayer = BridgePlayerManager.getInstance().getOnlinePlayer(uuid);

        // checking if the player is on a proxy managed by CloudNet
        if (cloudPlayer != null && cloudPlayer.getLoginService() != null) {
            ServiceInfoSnapshot proxyService = Wrapper.getInstance().getCloudServiceProvider().getCloudService(cloudPlayer.getLoginService().getUniqueId());
            if (proxyService != null) {
                try {
                    InetAddress proxyAddress = InetAddress.getByName(proxyService.getAddress().getHost());

                    if (proxyAddress.isLoopbackAddress() || proxyAddress.isAnyLocalAddress()) {
                        Wrapper.getInstance().getLogger().warning("OnlyProxyProtection was disabled because it's not clear on which host your proxy is running. "
                                + "Please set a remote address by changing the 'hostAddress' property in the config.json of the node the proxy is running on.");
                        return true;
                    }

                    return playerAddress.equals(proxyAddress.getHostAddress());

                } catch (UnknownHostException exception) {
                    exception.printStackTrace();
                }
            }
        }

        return false;
    }

    public static void changeToIngame(Consumer<String> stateChanger) {
        stateChanger.accept("INGAME");
        BridgeHelper.updateServiceInfo();

        String task = Wrapper.getInstance().getServiceId().getTaskName();

        CloudNetDriver.getInstance().getServiceTaskProvider().getServiceTaskAsync(task).onComplete(serviceTask -> {
            if (serviceTask != null) {
                CloudNetDriver.getInstance().getCloudServiceFactory().createCloudServiceAsync(serviceTask).onComplete(serviceInfoSnapshot -> {
                    if (serviceInfoSnapshot != null) {
                        serviceInfoSnapshot.provider().start();
                    }
                });
            }
        });
    }


    public static ServiceInfoSnapshot filterServiceForPlayer(String currentServer, BiFunction<String, String, List<Map.Entry<String, ServiceInfoSnapshot>>> filteredEntries,
                                                             Predicate<String> permissionCheck) {
        AtomicReference<ServiceInfoSnapshot> server = new AtomicReference<>();

        BridgeConfigurationProvider.load().getBungeeFallbackConfigurations().stream()
                .filter(
                        proxyFallbackConfiguration ->
                                proxyFallbackConfiguration.getTargetGroup() != null &&
                                        Arrays.asList(Wrapper.getInstance().getCurrentServiceInfoSnapshot().getConfiguration().getGroups())
                                                .contains(proxyFallbackConfiguration.getTargetGroup())
                )
                .forEach(configuration -> {
                    List<ProxyFallback> proxyFallbacks = configuration.getFallbacks();
                    Collections.sort(proxyFallbacks);

                    for (ProxyFallback proxyFallback : proxyFallbacks) {
                        if (server.get() != null)
                            break;
                        if (proxyFallback.getTask() == null || (proxyFallback.getPermission() != null && !permissionCheck.test(proxyFallback.getPermission()))) {
                            continue;
                        }

                        filteredEntries.apply(proxyFallback.getTask(), currentServer)
                                .stream()
                                .map(Map.Entry::getValue).min(Comparator.comparingInt(ServiceInfoSnapshotUtil::getOnlineCount))
                                .ifPresent(server::set);
                    }

                    if (server.get() == null) {
                        filteredEntries.apply(configuration.getDefaultFallbackTask(), currentServer)
                                .stream()
                                .map(Map.Entry::getValue).min(Comparator.comparingInt(ServiceInfoSnapshotUtil::getOnlineCount))
                                .ifPresent(server::set);
                    }
                });

        return server.get();
    }

    public static boolean isFallbackService(ServiceInfoSnapshot serviceInfoSnapshot) {
        for (ProxyFallbackConfiguration bungeeFallbackConfiguration : BridgeConfigurationProvider.load().getBungeeFallbackConfigurations()) {
            if (bungeeFallbackConfiguration.getTargetGroup() != null &&
                    Arrays.asList(Wrapper.getInstance().getCurrentServiceInfoSnapshot().getConfiguration().getGroups()).contains(bungeeFallbackConfiguration.getTargetGroup()
                    )) {
                for (ProxyFallback bungeeFallback : bungeeFallbackConfiguration.getFallbacks()) {
                    if (bungeeFallback.getTask() != null && serviceInfoSnapshot.getServiceId().getTaskName().equals(bungeeFallback.getTask())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

}