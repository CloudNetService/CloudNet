package de.dytanic.cloudnet.ext.bridge.listener;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;
import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import de.dytanic.cloudnet.ext.bridge.BridgeConstants;
import de.dytanic.cloudnet.ext.bridge.event.*;
import de.dytanic.cloudnet.ext.bridge.player.*;

public final class BridgeCustomChannelMessageListener {

    @EventListener
    public void handlePlayerUpdatesReceive(ChannelMessageReceiveEvent event)
    {
        if (!event.getChannel().equalsIgnoreCase(BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME))
            return;

        switch (event.getMessage().toLowerCase())
        {
            case "update_offline_cloud_player":
            {
                ICloudOfflinePlayer offlineCloudPlayer = event.getData().get("offlineCloudPlayer", CloudOfflinePlayer.TYPE);
                CloudNetDriver.getInstance().getEventManager().callEvent(new BridgeUpdateCloudOfflinePlayerEvent(offlineCloudPlayer));
            }
            break;
            case "update_online_cloud_player":
            {
                ICloudPlayer cloudPlayer = event.getData().get("cloudPlayer", CloudPlayer.TYPE);
                CloudNetDriver.getInstance().getEventManager().callEvent(new BridgeUpdateCloudPlayerEvent(cloudPlayer));
            }
            break;
        }
    }

    @EventListener
    public void handle(ChannelMessageReceiveEvent event)
    {
        if (!event.getChannel().equalsIgnoreCase(BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL))
            return;

        switch (event.getMessage().toLowerCase())
        {
            case "update_bridge_configuration":
            {
                BridgeConfiguration bridgeConfiguration = event.getData().get("bridgeConfiguration", BridgeConfiguration.TYPE);

                if (bridgeConfiguration != null)
                {
                    BridgeConfigurationProvider.setLocal(bridgeConfiguration);
                    CloudNetDriver.getInstance().getEventManager().callEvent(new BridgeConfigurationUpdateEvent(bridgeConfiguration));
                }
            }
            break;
            case BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_LOGIN_REQUEST:
            {
                NetworkConnectionInfo networkConnectionInfo = event.getData().get("networkConnectionInfo", NetworkConnectionInfo.TYPE);
                CloudNetDriver.getInstance().getEventManager().callEvent(new BridgeProxyPlayerLoginRequestEvent(networkConnectionInfo));
            }
            break;
            case BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_LOGIN_SUCCESS:
            {
                NetworkConnectionInfo networkConnectionInfo = event.getData().get("networkConnectionInfo", NetworkConnectionInfo.TYPE);
                CloudNetDriver.getInstance().getEventManager().callEvent(new BridgeProxyPlayerLoginSuccessEvent(networkConnectionInfo));
            }
            break;
            case BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_DISCONNECT:
            {
                NetworkConnectionInfo networkConnectionInfo = event.getData().get("networkConnectionInfo", NetworkConnectionInfo.TYPE);
                CloudNetDriver.getInstance().getEventManager().callEvent(new BridgeProxyPlayerDisconnectEvent(networkConnectionInfo));
            }
            break;
            case BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_SERVER_CONNECT_REQUEST:
            {
                NetworkConnectionInfo networkConnectionInfo = event.getData().get("networkConnectionInfo", NetworkConnectionInfo.TYPE);
                NetworkServiceInfo networkServiceInfo = event.getData().get("networkServiceInfo", NetworkServiceInfo.class);
                CloudNetDriver.getInstance().getEventManager().callEvent(new BridgeProxyPlayerServerConnectRequestEvent(networkConnectionInfo, networkServiceInfo));
            }
            break;
            case BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_SERVER_SWITCH:
            {
                NetworkConnectionInfo networkConnectionInfo = event.getData().get("networkConnectionInfo", NetworkConnectionInfo.TYPE);
                NetworkServiceInfo networkServiceInfo = event.getData().get("networkServiceInfo", NetworkServiceInfo.class);
                CloudNetDriver.getInstance().getEventManager().callEvent(new BridgeProxyPlayerServerSwitchEvent(networkConnectionInfo, networkServiceInfo));
            }
            break;
            case BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_SERVER_LOGIN_REQUEST:
            {
                NetworkConnectionInfo networkConnectionInfo = event.getData().get("networkConnectionInfo", NetworkConnectionInfo.TYPE);
                NetworkPlayerServerInfo networkPlayerServerInfo = event.getData().get("networkPlayerServerInfo", NetworkPlayerServerInfo.TYPE);
                CloudNetDriver.getInstance().getEventManager().callEvent(new BridgeServerPlayerLoginRequestEvent(networkConnectionInfo, networkPlayerServerInfo));
            }
            break;
            case BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_SERVER_LOGIN_SUCCESS:
            {
                NetworkConnectionInfo networkConnectionInfo = event.getData().get("networkConnectionInfo", NetworkConnectionInfo.TYPE);
                NetworkPlayerServerInfo networkPlayerServerInfo = event.getData().get("networkPlayerServerInfo", NetworkPlayerServerInfo.TYPE);
                CloudNetDriver.getInstance().getEventManager().callEvent(new BridgeServerPlayerLoginSuccessEvent(networkConnectionInfo, networkPlayerServerInfo));
            }
            break;
            case BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_SERVER_DISCONNECT:
            {
                NetworkConnectionInfo networkConnectionInfo = event.getData().get("networkConnectionInfo", NetworkConnectionInfo.TYPE);
                NetworkPlayerServerInfo networkPlayerServerInfo = event.getData().get("networkPlayerServerInfo", NetworkPlayerServerInfo.TYPE);
                CloudNetDriver.getInstance().getEventManager().callEvent(new BridgeServerPlayerDisconnectEvent(networkConnectionInfo, networkPlayerServerInfo));
            }
            break;
        }
    }
}