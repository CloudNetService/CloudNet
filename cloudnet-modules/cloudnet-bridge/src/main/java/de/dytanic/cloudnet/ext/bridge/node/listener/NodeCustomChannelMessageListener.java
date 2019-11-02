package de.dytanic.cloudnet.ext.bridge.node.listener;

import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;
import de.dytanic.cloudnet.ext.bridge.BridgeConstants;
import de.dytanic.cloudnet.ext.bridge.event.*;
import de.dytanic.cloudnet.ext.bridge.node.CloudNetBridgeModule;
import de.dytanic.cloudnet.ext.bridge.node.player.NodePlayerManager;
import de.dytanic.cloudnet.ext.bridge.player.*;

import java.io.File;

public final class NodeCustomChannelMessageListener {
    private BridgeConfiguration bridgeConfiguration;

    public NodeCustomChannelMessageListener() {
        bridgeConfiguration = CloudNetBridgeModule.getInstance().getBridgeConfiguration();
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
                CloudNetDriver.getInstance().getEventManager().callEvent(new BridgeProxyPlayerLoginRequestEvent(networkConnectionInfo));

                if (bridgeConfiguration.isLogPlayerConnections()) {
                    System.out.println(LanguageManager.getMessage("module-bridge-player-proxy-login-request")
                            .replace("%uniqueId%", String.valueOf(networkConnectionInfo.getUniqueId()))
                            .replace("%name%", networkConnectionInfo.getName())
                            .replace("%proxy%", networkConnectionInfo.getNetworkService().getServerName())
                    );
                }
            }
            break;
            case BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_LOGIN_SUCCESS: {
                NetworkConnectionInfo networkConnectionInfo = event.getData().get("networkConnectionInfo", NetworkConnectionInfo.TYPE);
                CloudNetDriver.getInstance().getEventManager().callEvent(new BridgeProxyPlayerLoginSuccessEvent(networkConnectionInfo));

                if (bridgeConfiguration.isLogPlayerConnections()) {
                    System.out.println(LanguageManager.getMessage("module-bridge-player-proxy-login-success")
                            .replace("%uniqueId%", String.valueOf(networkConnectionInfo.getUniqueId()))
                            .replace("%name%", networkConnectionInfo.getName())
                            .replace("%proxy%", networkConnectionInfo.getNetworkService().getServerName())
                    );
                }

                //Player
                loginPlayer(networkConnectionInfo, null);
            }
            break;
            case BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_SERVER_CONNECT_REQUEST: {
                NetworkConnectionInfo networkConnectionInfo = event.getData().get("networkConnectionInfo", NetworkConnectionInfo.TYPE);
                NetworkServiceInfo networkServiceInfo = event.getData().get("networkServiceInfo", NetworkServiceInfo.class);

                if (bridgeConfiguration.isLogPlayerConnections()) {
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

                if (bridgeConfiguration.isLogPlayerConnections()) {
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

                if (bridgeConfiguration.isLogPlayerConnections()) {
                    System.out.println(LanguageManager.getMessage("module-bridge-player-proxy-disconnect")
                            .replace("%uniqueId%", String.valueOf(networkConnectionInfo.getUniqueId()))
                            .replace("%name%", networkConnectionInfo.getName())
                            .replace("%proxy%", networkConnectionInfo.getNetworkService().getServerName())
                    );
                }

                CloudNetDriver.getInstance().getEventManager().callEvent(new BridgeProxyPlayerDisconnectEvent(networkConnectionInfo));
                logoutPlayer(networkConnectionInfo);
            }
            break;
            case BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_SERVER_LOGIN_REQUEST: {
                NetworkPlayerServerInfo networkPlayerServerInfo = event.getData().get("networkPlayerServerInfo", NetworkPlayerServerInfo.TYPE);
                NetworkConnectionInfo networkConnectionInfo = event.getData().get("networkConnectionInfo", NetworkConnectionInfo.TYPE);

                if (bridgeConfiguration.isLogPlayerConnections()) {
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

                if (bridgeConfiguration.isLogPlayerConnections()) {
                    System.out.println(LanguageManager.getMessage("module-bridge-player-server-login-success")
                            .replace("%uniqueId%", String.valueOf(networkPlayerServerInfo.getUniqueId()))
                            .replace("%name%", networkPlayerServerInfo.getName())
                            .replace("%server%", networkPlayerServerInfo.getNetworkService().getServerName())
                    );
                }

                CloudNetDriver.getInstance().getEventManager().callEvent(new BridgeServerPlayerLoginSuccessEvent(networkConnectionInfo, networkPlayerServerInfo));
                loginPlayer(networkConnectionInfo, networkPlayerServerInfo);
            }
            break;
            case BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_SERVER_DISCONNECT: {
                NetworkPlayerServerInfo networkPlayerServerInfo = event.getData().get("networkPlayerServerInfo", NetworkPlayerServerInfo.TYPE);
                NetworkConnectionInfo networkConnectionInfo = event.getData().get("networkConnectionInfo", NetworkConnectionInfo.TYPE);

                if (bridgeConfiguration.isLogPlayerConnections()) {
                    System.out.println(LanguageManager.getMessage("module-bridge-player-server-disconnect")
                            .replace("%uniqueId%", String.valueOf(networkPlayerServerInfo.getUniqueId()))
                            .replace("%name%", networkPlayerServerInfo.getName())
                            .replace("%server%", networkPlayerServerInfo.getNetworkService().getServerName())
                    );
                }

                CloudNetDriver.getInstance().getEventManager().callEvent(new BridgeServerPlayerDisconnectEvent(networkConnectionInfo, networkPlayerServerInfo));
                logoutPlayer(networkConnectionInfo);
            }
            break;
        }
    }


    private void loginPlayer(NetworkConnectionInfo networkConnectionInfo, NetworkPlayerServerInfo networkPlayerServerInfo) {
        CloudPlayer cloudPlayer = NodePlayerManager.getInstance().getOnlinePlayer(networkConnectionInfo.getUniqueId());

        if (cloudPlayer == null) {
            cloudPlayer = Iterables.first(NodePlayerManager.getInstance().getOnlineCloudPlayers().values(), cloudPlayer1 -> cloudPlayer1.getName().equalsIgnoreCase(networkConnectionInfo.getName()) &&
                    cloudPlayer1.getLoginService().getUniqueId().equals(networkConnectionInfo.getNetworkService().getUniqueId()));

            if (cloudPlayer == null) {
                ICloudOfflinePlayer cloudOfflinePlayer = getOrRegisterOfflinePlayer(networkConnectionInfo);

                cloudPlayer = new CloudPlayer(
                        cloudOfflinePlayer,
                        networkConnectionInfo.getNetworkService(),
                        networkConnectionInfo.getNetworkService(),
                        networkConnectionInfo,
                        networkPlayerServerInfo
                );

                cloudPlayer.setLastLoginTimeMillis(System.currentTimeMillis());
                NodePlayerManager.getInstance().getOnlineCloudPlayers().put(cloudPlayer.getUniqueId(), cloudPlayer);
            }
        }

        if (networkPlayerServerInfo != null) {
            cloudPlayer.setConnectedService(networkPlayerServerInfo.getNetworkService());
            cloudPlayer.setNetworkPlayerServerInfo(networkPlayerServerInfo);

            if (networkPlayerServerInfo.getXBoxId() != null) {
                cloudPlayer.setXBoxId(networkPlayerServerInfo.getXBoxId());
            }
        }

        cloudPlayer.setName(networkConnectionInfo.getName());

        NodePlayerManager.getInstance().updateOnlinePlayer0(cloudPlayer);
    }

    private ICloudOfflinePlayer getOrRegisterOfflinePlayer(NetworkConnectionInfo networkConnectionInfo) {
        ICloudOfflinePlayer cloudOfflinePlayer = NodePlayerManager.getInstance().getOfflinePlayer(networkConnectionInfo.getUniqueId());

        if (cloudOfflinePlayer == null) {
            cloudOfflinePlayer = new CloudOfflinePlayer(
                    networkConnectionInfo.getUniqueId(),
                    networkConnectionInfo.getName(),
                    null,
                    System.currentTimeMillis(),
                    System.currentTimeMillis(),
                    networkConnectionInfo
            );

            NodePlayerManager.getInstance().getDatabase().insert(
                    cloudOfflinePlayer.getUniqueId().toString(),
                    JsonDocument.newDocument(cloudOfflinePlayer)
            );
        }

        return cloudOfflinePlayer;
    }

    private void logoutPlayer(NetworkConnectionInfo networkConnectionInfo) {
        CloudPlayer cloudPlayer = networkConnectionInfo.getUniqueId() != null ?
                NodePlayerManager.getInstance().getOnlinePlayer(networkConnectionInfo.getUniqueId()) :
                Iterables.first(NodePlayerManager.getInstance().getOnlineCloudPlayers().values(), cloudPlayer1 -> cloudPlayer1.getName().equalsIgnoreCase(networkConnectionInfo.getName()));

        if (cloudPlayer != null) {
            if (cloudPlayer.getLoginService().getUniqueId().equals(networkConnectionInfo.getNetworkService().getUniqueId())) {
                cloudPlayer.setLastNetworkConnectionInfo(cloudPlayer.getNetworkConnectionInfo());
                NodePlayerManager.getInstance().updateOnlinePlayer0(cloudPlayer);
                NodePlayerManager.getInstance().getOnlineCloudPlayers().remove(cloudPlayer.getUniqueId());
            }
        }
    }
}
