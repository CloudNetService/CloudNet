package de.dytanic.cloudnet.ext.bridge;

import de.dytanic.cloudnet.common.annotation.UnsafeClass;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkPlayerServerInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkServiceInfo;
import de.dytanic.cloudnet.ext.bridge.proxy.BridgeProxyHelper;
import de.dytanic.cloudnet.wrapper.Wrapper;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;

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

    public static JsonDocument sendChannelMessageProxyLoginRequest(NetworkConnectionInfo networkConnectionInfo) {
        try {
            return CloudNetDriver.getInstance().getPacketQueryProvider().sendCallablePacket(
                    CloudNetDriver.getInstance().getNetworkClient().getChannels().iterator().next(),
                    BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL,
                    BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_LOGIN_REQUEST,
                    new JsonDocument("networkConnectionInfo", networkConnectionInfo),
                    Function.identity()
            ).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
        return null;
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