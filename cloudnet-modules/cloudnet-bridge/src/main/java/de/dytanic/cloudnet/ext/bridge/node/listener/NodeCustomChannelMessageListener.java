package de.dytanic.cloudnet.ext.bridge.node.listener;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.event.network.NetworkChannelReceiveCallablePacketEvent;
import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;
import de.dytanic.cloudnet.ext.bridge.BridgeConstants;
import de.dytanic.cloudnet.ext.bridge.event.*;
import de.dytanic.cloudnet.ext.bridge.node.CloudNetBridgeModule;
import de.dytanic.cloudnet.ext.bridge.node.event.NodeLocalBridgePlayerProxyLoginRequestEvent;
import de.dytanic.cloudnet.ext.bridge.node.player.NodePlayerManager;
import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkPlayerServerInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkServiceInfo;

import java.io.File;

public final class NodeCustomChannelMessageListener {

    private final NodePlayerManager nodePlayerManager;
    private final BridgeConfiguration bridgeConfiguration;

    public NodeCustomChannelMessageListener(NodePlayerManager nodePlayerManager) {
        this.nodePlayerManager = nodePlayerManager;
        this.bridgeConfiguration = CloudNetBridgeModule.getInstance().getBridgeConfiguration();
    }

    @EventListener
    public void handle(NetworkChannelReceiveCallablePacketEvent event) {
        if (!event.getChannelName().equalsIgnoreCase(BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL)) {
            return;
        }

        if (BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_LOGIN_REQUEST.equals(event.getId())) {
            NetworkConnectionInfo networkConnectionInfo = event.getHeader().get("networkConnectionInfo", NetworkConnectionInfo.TYPE);
            event.setCallbackPacket(this.processLoginRequest(new NodeLocalBridgePlayerProxyLoginRequestEvent(networkConnectionInfo, null)));

            this.callProxyLoginRequest(networkConnectionInfo);

            CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
                    BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL,
                    BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_LOGIN_REQUEST,
                    new JsonDocument("networkConnectionInfo", networkConnectionInfo)
            );
        }
    }

    private JsonDocument processLoginRequest(NodeLocalBridgePlayerProxyLoginRequestEvent requestEvent) {
        CloudNetDriver.getInstance().getEventManager().callEvent(requestEvent);

        if (this.nodePlayerManager.getOnlinePlayer(requestEvent.getConnectionInfo().getUniqueId()) != null) {
            requestEvent.setCancelled(true);
            requestEvent.setKickReason(this.bridgeConfiguration.getMessages().get("already-connected"));
        }

        if (requestEvent.isCancelled()) {
            if (requestEvent.getKickReason() == null) {
                requestEvent.setKickReason("§cNo kick reason given");
            }
            return JsonDocument.newDocument("kickReason", requestEvent.getKickReason());
        }
        return JsonDocument.newDocument();
    }

    private void callProxyLoginRequest(NetworkConnectionInfo networkConnectionInfo) {
        CloudNetDriver.getInstance().getEventManager().callEvent(new BridgeProxyPlayerLoginRequestEvent(networkConnectionInfo));

        if (this.bridgeConfiguration.isLogPlayerConnections()) {
            System.out.println(LanguageManager.getMessage("module-bridge-player-proxy-login-request")
                    .replace("%uniqueId%", String.valueOf(networkConnectionInfo.getUniqueId()))
                    .replace("%name%", networkConnectionInfo.getName())
                    .replace("%proxy%", networkConnectionInfo.getNetworkService().getServerName())
            );
        }
    }

    @EventListener
    public void handle(ChannelMessageReceiveEvent event) {
        if (!event.getChannel().equalsIgnoreCase(BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL)) {
            return;
        }

        switch (event.getMessage().toLowerCase()) {
            case BridgeConstants.BRIDGE_NETWORK_CHANNEL_CLUSTER_MESSAGE_UPDATE_BRIDGE_CONFIGURATION_LISTENER: {
                BridgeConfiguration bridgeConfiguration = event.getData().get("bridgeConfiguration", BridgeConfiguration.TYPE);

                if (bridgeConfiguration != null) {
                    new JsonDocument()
                            .append("config", bridgeConfiguration
                            ).write(new File(CloudNetBridgeModule.getInstance().getModuleWrapper().getDataFolder(), "config.json")
                    );

                    CloudNetBridgeModule.getInstance().setBridgeConfiguration(bridgeConfiguration);
                    CloudNetDriver.getInstance().getEventManager().callEvent(new BridgeConfigurationUpdateEvent(bridgeConfiguration));
                }
            }
            break;
            case BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_LOGIN_REQUEST: {
                NetworkConnectionInfo networkConnectionInfo = event.getData().get("networkConnectionInfo", NetworkConnectionInfo.TYPE);
                this.callProxyLoginRequest(networkConnectionInfo);
            }
            break;
            case BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_LOGIN_SUCCESS: {
                NetworkConnectionInfo networkConnectionInfo = event.getData().get("networkConnectionInfo", NetworkConnectionInfo.TYPE);
                CloudNetDriver.getInstance().getEventManager().callEvent(new BridgeProxyPlayerLoginSuccessEvent(networkConnectionInfo));

                if (this.bridgeConfiguration.isLogPlayerConnections()) {
                    System.out.println(LanguageManager.getMessage("module-bridge-player-proxy-login-success")
                            .replace("%uniqueId%", String.valueOf(networkConnectionInfo.getUniqueId()))
                            .replace("%name%", networkConnectionInfo.getName())
                            .replace("%proxy%", networkConnectionInfo.getNetworkService().getServerName())
                    );
                }

                this.nodePlayerManager.loginPlayer(networkConnectionInfo, null);
            }
            break;
            case BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_SERVER_CONNECT_REQUEST: {
                NetworkConnectionInfo networkConnectionInfo = event.getData().get("networkConnectionInfo", NetworkConnectionInfo.TYPE);
                NetworkServiceInfo networkServiceInfo = event.getData().get("networkServiceInfo", NetworkServiceInfo.class);

                if (this.bridgeConfiguration.isLogPlayerConnections()) {
                    System.out.println(LanguageManager.getMessage("module-bridge-player-proxy-server-connect-request")
                            .replace("%uniqueId%", String.valueOf(networkConnectionInfo.getUniqueId()))
                            .replace("%name%", networkConnectionInfo.getName())
                            .replace("%proxy%", networkConnectionInfo.getNetworkService().getServerName())
                            .replace("%server%", networkServiceInfo.getServerName())
                    );
                }

                CloudNetDriver.getInstance().getEventManager().callEvent(new BridgeProxyPlayerServerConnectRequestEvent(networkConnectionInfo, networkServiceInfo));
            }
            break;
            case BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_SERVER_SWITCH: {
                NetworkConnectionInfo networkConnectionInfo = event.getData().get("networkConnectionInfo", NetworkConnectionInfo.TYPE);
                NetworkServiceInfo networkServiceInfo = event.getData().get("networkServiceInfo", NetworkServiceInfo.class);

                if (this.bridgeConfiguration.isLogPlayerConnections()) {
                    System.out.println(LanguageManager.getMessage("module-bridge-player-proxy-server-switch")
                            .replace("%uniqueId%", String.valueOf(networkConnectionInfo.getUniqueId()))
                            .replace("%name%", networkConnectionInfo.getName())
                            .replace("%proxy%", networkConnectionInfo.getNetworkService().getServerName())
                            .replace("%server%", networkServiceInfo.getServerName())
                    );
                }

                CloudNetDriver.getInstance().getEventManager().callEvent(new BridgeProxyPlayerServerSwitchEvent(networkConnectionInfo, networkServiceInfo));
            }
            break;
            case BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_DISCONNECT: {
                NetworkConnectionInfo networkConnectionInfo = event.getData().get("networkConnectionInfo", NetworkConnectionInfo.TYPE);

                if (this.bridgeConfiguration.isLogPlayerConnections()) {
                    System.out.println(LanguageManager.getMessage("module-bridge-player-proxy-disconnect")
                            .replace("%uniqueId%", String.valueOf(networkConnectionInfo.getUniqueId()))
                            .replace("%name%", networkConnectionInfo.getName())
                            .replace("%proxy%", networkConnectionInfo.getNetworkService().getServerName())
                    );
                }

                CloudNetDriver.getInstance().getEventManager().callEvent(new BridgeProxyPlayerDisconnectEvent(networkConnectionInfo));
                this.nodePlayerManager.logoutPlayer(networkConnectionInfo);
            }
            break;
            case BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_SERVER_LOGIN_REQUEST: {
                NetworkPlayerServerInfo networkPlayerServerInfo = event.getData().get("networkPlayerServerInfo", NetworkPlayerServerInfo.TYPE);
                NetworkConnectionInfo networkConnectionInfo = event.getData().get("networkConnectionInfo", NetworkConnectionInfo.TYPE);

                if (this.bridgeConfiguration.isLogPlayerConnections()) {
                    System.out.println(LanguageManager.getMessage("module-bridge-player-server-login-request")
                            .replace("%uniqueId%", String.valueOf(networkPlayerServerInfo.getUniqueId()))
                            .replace("%name%", networkPlayerServerInfo.getName())
                            .replace("%server%", networkPlayerServerInfo.getNetworkService().getServerName())
                    );
                }

                CloudNetDriver.getInstance().getEventManager().callEvent(new BridgeServerPlayerLoginRequestEvent(networkConnectionInfo, networkPlayerServerInfo));
            }
            break;
            case BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_SERVER_LOGIN_SUCCESS: {
                NetworkPlayerServerInfo networkPlayerServerInfo = event.getData().get("networkPlayerServerInfo", NetworkPlayerServerInfo.TYPE);
                NetworkConnectionInfo networkConnectionInfo = event.getData().get("networkConnectionInfo", NetworkConnectionInfo.TYPE);

                if (this.bridgeConfiguration.isLogPlayerConnections()) {
                    System.out.println(LanguageManager.getMessage("module-bridge-player-server-login-success")
                            .replace("%uniqueId%", String.valueOf(networkPlayerServerInfo.getUniqueId()))
                            .replace("%name%", networkPlayerServerInfo.getName())
                            .replace("%server%", networkPlayerServerInfo.getNetworkService().getServerName())
                    );
                }

                CloudNetDriver.getInstance().getEventManager().callEvent(new BridgeServerPlayerLoginSuccessEvent(networkConnectionInfo, networkPlayerServerInfo));
                this.nodePlayerManager.loginPlayer(networkConnectionInfo, networkPlayerServerInfo);
            }
            break;
            case BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_SERVER_DISCONNECT: {
                NetworkPlayerServerInfo networkPlayerServerInfo = event.getData().get("networkPlayerServerInfo", NetworkPlayerServerInfo.TYPE);
                NetworkConnectionInfo networkConnectionInfo = event.getData().get("networkConnectionInfo", NetworkConnectionInfo.TYPE);

                if (this.bridgeConfiguration.isLogPlayerConnections()) {
                    System.out.println(LanguageManager.getMessage("module-bridge-player-server-disconnect")
                            .replace("%uniqueId%", String.valueOf(networkPlayerServerInfo.getUniqueId()))
                            .replace("%name%", networkPlayerServerInfo.getName())
                            .replace("%server%", networkPlayerServerInfo.getNetworkService().getServerName())
                    );
                }

                CloudNetDriver.getInstance().getEventManager().callEvent(new BridgeServerPlayerDisconnectEvent(networkConnectionInfo, networkPlayerServerInfo));
                this.nodePlayerManager.logoutPlayer(networkConnectionInfo);
            }
            break;
        }
    }


}
