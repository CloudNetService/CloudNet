package de.dytanic.cloudnet.ext.bridge;

import de.dytanic.cloudnet.common.annotation.UnsafeClass;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.player.*;
import de.dytanic.cloudnet.ext.bridge.proxy.BridgeProxyHelper;
import de.dytanic.cloudnet.wrapper.Wrapper;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.function.Consumer;

@UnsafeClass
public final class BridgeHelper {

    private static boolean online = true;

    private BridgeHelper() {
        throw new UnsupportedOperationException();
    }

    public static void setOnline(boolean online) {
        BridgeHelper.online = online;
    }

    public static boolean isOnline() {
        return BridgeHelper.online;
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
        ICloudPlayer cloudPlayer = CloudNetDriver.getInstance().getServicesRegistry().getService(IPlayerManager.class).getOnlinePlayer(uuid);

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

    /**
     * @deprecated  moved to {@link BridgeProxyHelper}
     */
    @Deprecated
    public static boolean isFallbackService(ServiceInfoSnapshot serviceInfoSnapshot) {
        return BridgeProxyHelper.isFallbackService(serviceInfoSnapshot);
    }

}