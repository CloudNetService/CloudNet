/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dytanic.cloudnet.ext.bridge.node.listener;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;
import de.dytanic.cloudnet.ext.bridge.BridgeConstants;
import de.dytanic.cloudnet.ext.bridge.event.BridgeConfigurationUpdateEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeProxyPlayerDisconnectEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeProxyPlayerLoginRequestEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeProxyPlayerLoginSuccessEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeProxyPlayerServerConnectRequestEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeProxyPlayerServerSwitchEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeServerPlayerDisconnectEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeServerPlayerLoginRequestEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeServerPlayerLoginSuccessEvent;
import de.dytanic.cloudnet.ext.bridge.node.CloudNetBridgeModule;
import de.dytanic.cloudnet.ext.bridge.node.event.NodeLocalBridgePlayerProxyLoginRequestEvent;
import de.dytanic.cloudnet.ext.bridge.node.player.NodePlayerManager;
import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkPlayerServerInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkServiceInfo;
import java.util.UUID;

public final class NodeCustomChannelMessageListener {

  private final NodePlayerManager nodePlayerManager;
  private final BridgeConfiguration bridgeConfiguration;

  public NodeCustomChannelMessageListener(NodePlayerManager nodePlayerManager) {
    this.nodePlayerManager = nodePlayerManager;
    this.bridgeConfiguration = CloudNetBridgeModule.getInstance().getBridgeConfiguration();
  }

  @EventListener
  public void handleProxyLoginRequest(ChannelMessageReceiveEvent event) {
    if (!event.getChannel().equalsIgnoreCase(BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL) || !event
      .isQuery()) {
      return;
    }

    if (BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_LOGIN_REQUEST.equals(event.getMessage())) {
      NetworkConnectionInfo networkConnectionInfo = event.getBuffer().readObject(NetworkConnectionInfo.class);
      ProtocolBuffer response = this
        .processLoginRequest(new NodeLocalBridgePlayerProxyLoginRequestEvent(networkConnectionInfo, null));
      event.setQueryResponse(ChannelMessage.buildResponseFor(event.getChannelMessage()).buffer(response).build());

      this.callProxyLoginRequest(networkConnectionInfo);

      this.nodePlayerManager.messageBuilder()
        .message(BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_LOGIN_REQUEST)
        .buffer(ProtocolBuffer.create().writeObject(networkConnectionInfo))
        .targetAll()
        .build()
        .send();
    }
  }

  private ProtocolBuffer processLoginRequest(NodeLocalBridgePlayerProxyLoginRequestEvent requestEvent) {
    CloudNetDriver.getInstance().getEventManager().callEvent(requestEvent);

    if (this.nodePlayerManager.getOnlinePlayer(requestEvent.getConnectionInfo().getUniqueId()) != null) {
      requestEvent.setCancelled(true);
      requestEvent.setKickReason(this.bridgeConfiguration.getMessages().get("already-connected"));
    }

    if (requestEvent.isCancelled()) {
      if (requestEvent.getKickReason() == null) {
        requestEvent.setKickReason("§cNo kick reason given");
      }
      return ProtocolBuffer.create().writeOptionalString(requestEvent.getKickReason());
    }
    return ProtocolBuffer.create().writeOptionalString(null);
  }

  private void callProxyLoginRequest(NetworkConnectionInfo networkConnectionInfo) {
    CloudNetDriver.getInstance().getEventManager()
      .callEvent(new BridgeProxyPlayerLoginRequestEvent(networkConnectionInfo));

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
    if (!event.getChannel().equals(BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL)
      || event.getMessage() == null) {
      return;
    }

    switch (event.getMessage()) {
      case BridgeConstants.BRIDGE_NETWORK_CHANNEL_CLUSTER_MESSAGE_UPDATE_BRIDGE_CONFIGURATION_LISTENER: {
        BridgeConfiguration bridgeConfiguration = event.getData().get("bridgeConfiguration", BridgeConfiguration.TYPE);

        if (bridgeConfiguration != null) {
          new JsonDocument()
            .append("config", bridgeConfiguration)
            .write(CloudNetBridgeModule.getInstance().getModuleWrapper().getDataDirectory().resolve("config.json"));

          CloudNetBridgeModule.getInstance().setBridgeConfiguration(bridgeConfiguration);
          CloudNetDriver.getInstance().getEventManager()
            .callEvent(new BridgeConfigurationUpdateEvent(bridgeConfiguration));
        }
      }
      break;
      case BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_LOGIN_SUCCESS: {
        NetworkConnectionInfo networkConnectionInfo = event.getBuffer().readObject(NetworkConnectionInfo.class);
        CloudNetDriver.getInstance().getEventManager()
          .callEvent(new BridgeProxyPlayerLoginSuccessEvent(networkConnectionInfo));

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
        NetworkConnectionInfo networkConnectionInfo = event.getBuffer().readObject(NetworkConnectionInfo.class);
        NetworkServiceInfo networkServiceInfo = event.getBuffer().readObject(NetworkServiceInfo.class);

        if (this.bridgeConfiguration.isLogPlayerConnections()) {
          System.out.println(LanguageManager.getMessage("module-bridge-player-proxy-server-connect-request")
            .replace("%uniqueId%", String.valueOf(networkConnectionInfo.getUniqueId()))
            .replace("%name%", networkConnectionInfo.getName())
            .replace("%proxy%", networkConnectionInfo.getNetworkService().getServerName())
            .replace("%server%", networkServiceInfo.getServerName())
          );
        }

        CloudNetDriver.getInstance().getEventManager()
          .callEvent(new BridgeProxyPlayerServerConnectRequestEvent(networkConnectionInfo, networkServiceInfo));
      }
      break;
      case BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_SERVER_SWITCH: {
        NetworkConnectionInfo networkConnectionInfo = event.getBuffer().readObject(NetworkConnectionInfo.class);
        NetworkServiceInfo networkServiceInfo = event.getBuffer().readObject(NetworkServiceInfo.class);

        if (this.bridgeConfiguration.isLogPlayerConnections()) {
          System.out.println(LanguageManager.getMessage("module-bridge-player-proxy-server-switch")
            .replace("%uniqueId%", String.valueOf(networkConnectionInfo.getUniqueId()))
            .replace("%name%", networkConnectionInfo.getName())
            .replace("%proxy%", networkConnectionInfo.getNetworkService().getServerName())
            .replace("%server%", networkServiceInfo.getServerName())
          );
        }

        CloudNetDriver.getInstance().getEventManager()
          .callEvent(new BridgeProxyPlayerServerSwitchEvent(networkConnectionInfo, networkServiceInfo));
      }
      break;
      case BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_DISCONNECT: {
        NetworkConnectionInfo networkConnectionInfo = event.getBuffer().readObject(NetworkConnectionInfo.class);

        if (this.bridgeConfiguration.isLogPlayerConnections()) {
          System.out.println(LanguageManager.getMessage("module-bridge-player-proxy-disconnect")
            .replace("%uniqueId%", String.valueOf(networkConnectionInfo.getUniqueId()))
            .replace("%name%", networkConnectionInfo.getName())
            .replace("%proxy%", networkConnectionInfo.getNetworkService().getServerName())
          );
        }

        CloudNetDriver.getInstance().getEventManager()
          .callEvent(new BridgeProxyPlayerDisconnectEvent(networkConnectionInfo));
        this.nodePlayerManager.logoutPlayer(networkConnectionInfo);
      }
      break;
      case BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_MISSING_DISCONNECT: {
        UUID playerId = event.getBuffer().readUUID();
        String playerName = event.getBuffer().readString();
        NetworkServiceInfo proxy = event.getBuffer().readObject(NetworkServiceInfo.class);

        if (this.bridgeConfiguration.isLogPlayerConnections()) {
          System.out.println(LanguageManager.getMessage("module-bridge-player-proxy-disconnect")
            .replace("%uniqueId%", String.valueOf(playerId))
            .replace("%name%", playerName)
            .replace("%proxy%", proxy.getServerName())
          );
        }

        this.nodePlayerManager.logoutPlayer(playerId, playerName, proxy);
      }
      break;
      case BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_SERVER_LOGIN_REQUEST: {
        NetworkConnectionInfo networkConnectionInfo = event.getBuffer().readObject(NetworkConnectionInfo.class);
        NetworkPlayerServerInfo networkPlayerServerInfo = event.getBuffer().readObject(NetworkPlayerServerInfo.class);

        if (this.bridgeConfiguration.isLogPlayerConnections()) {
          System.out.println(LanguageManager.getMessage("module-bridge-player-server-login-request")
            .replace("%uniqueId%", String.valueOf(networkPlayerServerInfo.getUniqueId()))
            .replace("%name%", networkPlayerServerInfo.getName())
            .replace("%server%", networkPlayerServerInfo.getNetworkService().getServerName())
          );
        }

        CloudNetDriver.getInstance().getEventManager()
          .callEvent(new BridgeServerPlayerLoginRequestEvent(networkConnectionInfo, networkPlayerServerInfo));
      }
      break;
      case BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_SERVER_LOGIN_SUCCESS: {
        NetworkConnectionInfo networkConnectionInfo = event.getBuffer().readObject(NetworkConnectionInfo.class);
        NetworkPlayerServerInfo networkPlayerServerInfo = event.getBuffer().readObject(NetworkPlayerServerInfo.class);

        if (this.bridgeConfiguration.isLogPlayerConnections()) {
          System.out.println(LanguageManager.getMessage("module-bridge-player-server-login-success")
            .replace("%uniqueId%", String.valueOf(networkPlayerServerInfo.getUniqueId()))
            .replace("%name%", networkPlayerServerInfo.getName())
            .replace("%server%", networkPlayerServerInfo.getNetworkService().getServerName())
          );
        }

        CloudNetDriver.getInstance().getEventManager()
          .callEvent(new BridgeServerPlayerLoginSuccessEvent(networkConnectionInfo, networkPlayerServerInfo));
        this.nodePlayerManager.loginPlayer(networkConnectionInfo, networkPlayerServerInfo);
      }
      break;
      case BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_SERVER_DISCONNECT: {
        NetworkConnectionInfo networkConnectionInfo = event.getBuffer().readObject(NetworkConnectionInfo.class);
        NetworkPlayerServerInfo networkPlayerServerInfo = event.getBuffer().readObject(NetworkPlayerServerInfo.class);

        if (this.bridgeConfiguration.isLogPlayerConnections()) {
          System.out.println(LanguageManager.getMessage("module-bridge-player-server-disconnect")
            .replace("%uniqueId%", String.valueOf(networkPlayerServerInfo.getUniqueId()))
            .replace("%name%", networkPlayerServerInfo.getName())
            .replace("%server%", networkPlayerServerInfo.getNetworkService().getServerName())
          );
        }

        CloudNetDriver.getInstance().getEventManager()
          .callEvent(new BridgeServerPlayerDisconnectEvent(networkConnectionInfo, networkPlayerServerInfo));
        this.nodePlayerManager.logoutPlayer(networkConnectionInfo);
      }
      break;
      default:
        break;
    }
  }


}
