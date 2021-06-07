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

package de.dytanic.cloudnet.ext.bridge.listener;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;
import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import de.dytanic.cloudnet.ext.bridge.BridgeConstants;
import de.dytanic.cloudnet.ext.bridge.event.BridgeConfigurationUpdateEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeDeleteCloudOfflinePlayerEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeProxyPlayerDisconnectEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeProxyPlayerLoginRequestEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeProxyPlayerLoginSuccessEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeProxyPlayerServerConnectRequestEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeProxyPlayerServerSwitchEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeServerPlayerDisconnectEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeServerPlayerLoginRequestEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeServerPlayerLoginSuccessEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeUpdateCloudOfflinePlayerEvent;
import de.dytanic.cloudnet.ext.bridge.event.BridgeUpdateCloudPlayerEvent;
import de.dytanic.cloudnet.ext.bridge.player.CloudOfflinePlayer;
import de.dytanic.cloudnet.ext.bridge.player.CloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.ICloudOfflinePlayer;
import de.dytanic.cloudnet.ext.bridge.player.ICloudPlayer;
import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkPlayerServerInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkServiceInfo;

public final class BridgeCustomChannelMessageListener {

  @EventListener
  public void handlePlayerUpdatesReceive(ChannelMessageReceiveEvent event) {
    if (!event.getChannel().equalsIgnoreCase(BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL)
      || event.getMessage() == null) {
      return;
    }

    switch (event.getMessage().toLowerCase()) {
      case "update_offline_cloud_player": {
        ICloudOfflinePlayer offlineCloudPlayer = event.getBuffer().readObject(CloudOfflinePlayer.class);
        CloudNetDriver.getInstance().getEventManager()
          .callEvent(new BridgeUpdateCloudOfflinePlayerEvent(offlineCloudPlayer));
      }
      break;
      case "update_online_cloud_player": {
        ICloudPlayer cloudPlayer = event.getBuffer().readObject(CloudPlayer.class);
        CloudNetDriver.getInstance().getEventManager().callEvent(new BridgeUpdateCloudPlayerEvent(cloudPlayer));
      }
      break;
      case "delete_offline_player": {
        ICloudOfflinePlayer offlinePlayer = event.getBuffer().readObject(CloudOfflinePlayer.class);
        CloudNetDriver.getInstance().getEventManager()
          .callEvent(new BridgeDeleteCloudOfflinePlayerEvent(offlinePlayer));
      }
      break;
      default:
        break;
    }
  }

  @EventListener
  public void handle(ChannelMessageReceiveEvent event) {
    if (!event.getChannel().equalsIgnoreCase(BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL)
      || event.getMessage() == null) {
      return;
    }

    switch (event.getMessage()) {
      case "update_bridge_configuration": {
        BridgeConfiguration bridgeConfiguration = event.getData().get("bridgeConfiguration", BridgeConfiguration.TYPE);

        if (bridgeConfiguration != null) {
          BridgeConfigurationProvider.setLocal(bridgeConfiguration);
          CloudNetDriver.getInstance().getEventManager()
            .callEvent(new BridgeConfigurationUpdateEvent(bridgeConfiguration));
        }
      }
      break;
      case BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_LOGIN_REQUEST: {
        NetworkConnectionInfo networkConnectionInfo = event.getBuffer().readObject(NetworkConnectionInfo.class);
        CloudNetDriver.getInstance().getEventManager()
          .callEvent(new BridgeProxyPlayerLoginRequestEvent(networkConnectionInfo));
      }
      break;
      case BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_LOGIN_SUCCESS: {
        NetworkConnectionInfo networkConnectionInfo = event.getBuffer().readObject(NetworkConnectionInfo.class);
        CloudNetDriver.getInstance().getEventManager()
          .callEvent(new BridgeProxyPlayerLoginSuccessEvent(networkConnectionInfo));
      }
      break;
      case BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_DISCONNECT: {
        NetworkConnectionInfo networkConnectionInfo = event.getBuffer().readObject(NetworkConnectionInfo.class);
        CloudNetDriver.getInstance().getEventManager()
          .callEvent(new BridgeProxyPlayerDisconnectEvent(networkConnectionInfo));
      }
      break;
      case BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_SERVER_CONNECT_REQUEST: {
        NetworkConnectionInfo networkConnectionInfo = event.getBuffer().readObject(NetworkConnectionInfo.class);
        NetworkServiceInfo networkServiceInfo = event.getBuffer().readObject(NetworkServiceInfo.class);
        CloudNetDriver.getInstance().getEventManager()
          .callEvent(new BridgeProxyPlayerServerConnectRequestEvent(networkConnectionInfo, networkServiceInfo));
      }
      break;
      case BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_SERVER_SWITCH: {
        NetworkConnectionInfo networkConnectionInfo = event.getBuffer().readObject(NetworkConnectionInfo.class);
        NetworkServiceInfo networkServiceInfo = event.getBuffer().readObject(NetworkServiceInfo.class);
        CloudNetDriver.getInstance().getEventManager()
          .callEvent(new BridgeProxyPlayerServerSwitchEvent(networkConnectionInfo, networkServiceInfo));
      }
      break;
      case BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_SERVER_LOGIN_REQUEST: {
        NetworkConnectionInfo networkConnectionInfo = event.getBuffer().readObject(NetworkConnectionInfo.class);
        NetworkPlayerServerInfo networkPlayerServerInfo = event.getBuffer().readObject(NetworkPlayerServerInfo.class);
        CloudNetDriver.getInstance().getEventManager()
          .callEvent(new BridgeServerPlayerLoginRequestEvent(networkConnectionInfo, networkPlayerServerInfo));
      }
      break;
      case BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_SERVER_LOGIN_SUCCESS: {
        NetworkConnectionInfo networkConnectionInfo = event.getBuffer().readObject(NetworkConnectionInfo.class);
        NetworkPlayerServerInfo networkPlayerServerInfo = event.getBuffer().readObject(NetworkPlayerServerInfo.class);
        CloudNetDriver.getInstance().getEventManager()
          .callEvent(new BridgeServerPlayerLoginSuccessEvent(networkConnectionInfo, networkPlayerServerInfo));
      }
      break;
      case BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_SERVER_DISCONNECT: {
        NetworkConnectionInfo networkConnectionInfo = event.getBuffer().readObject(NetworkConnectionInfo.class);
        NetworkPlayerServerInfo networkPlayerServerInfo = event.getBuffer().readObject(NetworkPlayerServerInfo.class);
        CloudNetDriver.getInstance().getEventManager()
          .callEvent(new BridgeServerPlayerDisconnectEvent(networkConnectionInfo, networkPlayerServerInfo));
      }
      break;
      default:
        break;
    }
  }
}
