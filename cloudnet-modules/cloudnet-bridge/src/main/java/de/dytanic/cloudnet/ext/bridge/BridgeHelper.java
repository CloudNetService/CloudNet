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
import java.util.UUID;

@UnsafeClass
public final class BridgeHelper {

    private BridgeHelper() {
        throw new UnsupportedOperationException();
    }

    public static void updateServiceInfo() {
        Wrapper.getInstance().publishServiceInfoUpdate();
    }

    public static void sendChannelMessageProxyLoginRequest(NetworkConnectionInfo networkConnectionInfo) {
        CloudNetDriver.getInstance().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL,
                BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_LOGIN_REQUEST,
                new JsonDocument("networkConnectionInfo", networkConnectionInfo)
        );
    }

    public static void sendChannelMessageProxyLoginSuccess(NetworkConnectionInfo networkConnectionInfo) {
        CloudNetDriver.getInstance().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL,
                BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_LOGIN_SUCCESS,
                new JsonDocument("networkConnectionInfo", networkConnectionInfo)
        );
    }

    public static void sendChannelMessageProxyDisconnect(NetworkConnectionInfo networkConnectionInfo) {
        CloudNetDriver.getInstance().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL,
                BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_DISCONNECT,
                new JsonDocument("networkConnectionInfo", networkConnectionInfo)
        );
    }

    public static void sendChannelMessageProxyServerSwitch(NetworkConnectionInfo networkConnectionInfo, NetworkServiceInfo networkServiceInfo) {
        CloudNetDriver.getInstance().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL,
                BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_SERVER_SWITCH,
                new JsonDocument("networkConnectionInfo", networkConnectionInfo)
                        .append("networkServiceInfo", networkServiceInfo)
        );
    }

    public static void sendChannelMessageProxyServerConnectRequest(NetworkConnectionInfo networkConnectionInfo, NetworkServiceInfo networkServiceInfo) {
        CloudNetDriver.getInstance().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL,
                BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_SERVER_CONNECT_REQUEST,
                new JsonDocument("networkConnectionInfo", networkConnectionInfo)
                        .append("networkServiceInfo", networkServiceInfo)
        );
    }

    public static void sendChannelMessageServerLoginRequest(NetworkConnectionInfo networkConnectionInfo, NetworkPlayerServerInfo networkPlayerServerInfo) {
        CloudNetDriver.getInstance().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL,
                BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_SERVER_LOGIN_REQUEST,
                new JsonDocument("networkConnectionInfo", networkConnectionInfo)
                        .append("networkPlayerServerInfo", networkPlayerServerInfo)
        );
    }

    public static void sendChannelMessageServerLoginSuccess(NetworkConnectionInfo networkConnectionInfo, NetworkPlayerServerInfo networkPlayerServerInfo) {
        CloudNetDriver.getInstance().sendChannelMessage(
                BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL,
                BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_SERVER_LOGIN_SUCCESS,
                new JsonDocument("networkConnectionInfo", networkConnectionInfo)
                        .append("networkPlayerServerInfo", networkPlayerServerInfo)
        );
    }

    public static void sendChannelMessageServerDisconnect(NetworkConnectionInfo networkConnectionInfo, NetworkPlayerServerInfo networkPlayerServerInfo) {
        CloudNetDriver.getInstance().sendChannelMessage(
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
            ServiceInfoSnapshot proxyService = Wrapper.getInstance().getCloudService(cloudPlayer.getLoginService().getUniqueId());
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

}