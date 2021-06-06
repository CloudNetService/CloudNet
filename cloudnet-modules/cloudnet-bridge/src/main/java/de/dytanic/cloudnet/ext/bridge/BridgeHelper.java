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

package de.dytanic.cloudnet.ext.bridge;

import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkPlayerServerInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkServiceInfo;
import de.dytanic.cloudnet.ext.bridge.player.ServicePlayer;
import de.dytanic.cloudnet.ext.bridge.proxy.BridgeProxyHelper;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.util.UUID;

public final class BridgeHelper {

  private static boolean online = true;

  private BridgeHelper() {
    throw new UnsupportedOperationException();
  }

  public static boolean isOnline() {
    return BridgeHelper.online;
  }

  public static void setOnline(boolean online) {
    BridgeHelper.online = online;
  }

  public static void updateServiceInfo() {
    Wrapper.getInstance().publishServiceInfoUpdate();
  }

  public static ChannelMessage.Builder messageBuilder() {
    return ChannelMessage.builder().channel(BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL);
  }

  public static NetworkServiceInfo createOwnNetworkServiceInfo() {
    return createNetworkServiceInfo(Wrapper.getInstance().getCurrentServiceInfoSnapshot());
  }

  public static NetworkServiceInfo createNetworkServiceInfo(ServiceInfoSnapshot serviceInfoSnapshot) {
    return new NetworkServiceInfo(
      serviceInfoSnapshot.getServiceId(),
      serviceInfoSnapshot.getConfiguration().getGroups()
    );
  }

  public static String sendChannelMessageProxyLoginRequest(NetworkConnectionInfo networkConnectionInfo) {
    ChannelMessage response = messageBuilder()
      .message(BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_LOGIN_REQUEST)
      .buffer(ProtocolBuffer.create().writeObject(networkConnectionInfo))
      .targetNode(Wrapper.getInstance().getServiceId().getNodeUniqueId())
      .build()
      .sendSingleQuery();
    return response != null ? response.getBuffer().readOptionalString() : null;
  }

  public static void sendChannelMessageProxyLoginSuccess(NetworkConnectionInfo networkConnectionInfo) {
    messageBuilder()
      .message(BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_LOGIN_SUCCESS)
      .buffer(ProtocolBuffer.create().writeObject(networkConnectionInfo))
      .targetAll()
      .build()
      .send();
  }

  public static void sendChannelMessageProxyDisconnect(NetworkConnectionInfo networkConnectionInfo) {
    messageBuilder()
      .message(BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_DISCONNECT)
      .buffer(ProtocolBuffer.create().writeObject(networkConnectionInfo))
      .targetAll()
      .build()
      .send();
  }

  public static void sendChannelMessageProxyServerSwitch(NetworkConnectionInfo networkConnectionInfo,
    NetworkServiceInfo networkServiceInfo) {
    messageBuilder()
      .message(BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_SERVER_SWITCH)
      .buffer(ProtocolBuffer.create().writeObject(networkConnectionInfo).writeObject(networkServiceInfo))
      .targetAll()
      .build()
      .send();
  }

  public static void sendChannelMessageProxyServerConnectRequest(NetworkConnectionInfo networkConnectionInfo,
    NetworkServiceInfo networkServiceInfo) {
    messageBuilder()
      .message(BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_SERVER_CONNECT_REQUEST)
      .buffer(ProtocolBuffer.create().writeObject(networkConnectionInfo).writeObject(networkServiceInfo))
      .targetAll()
      .build()
      .send();
  }

  public static void sendChannelMessageServerLoginRequest(NetworkConnectionInfo networkConnectionInfo,
    NetworkPlayerServerInfo networkPlayerServerInfo) {
    messageBuilder()
      .message(BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_SERVER_LOGIN_REQUEST)
      .buffer(ProtocolBuffer.create().writeObject(networkConnectionInfo).writeObject(networkPlayerServerInfo))
      .targetAll()
      .build()
      .send();
  }

  public static void sendChannelMessageServerLoginSuccess(NetworkConnectionInfo networkConnectionInfo,
    NetworkPlayerServerInfo networkPlayerServerInfo) {
    messageBuilder()
      .message(BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_SERVER_LOGIN_SUCCESS)
      .buffer(ProtocolBuffer.create().writeObject(networkConnectionInfo).writeObject(networkPlayerServerInfo))
      .targetAll()
      .build()
      .send();
  }

  public static void sendChannelMessageServerDisconnect(NetworkConnectionInfo networkConnectionInfo,
    NetworkPlayerServerInfo networkPlayerServerInfo) {
    messageBuilder()
      .message(BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_SERVER_DISCONNECT)
      .buffer(ProtocolBuffer.create().writeObject(networkConnectionInfo).writeObject(networkPlayerServerInfo))
      .targetAll()
      .build()
      .send();
  }

  public static void sendChannelMessageMissingDisconnect(ServicePlayer player) {
    messageBuilder()
      .message(BridgeConstants.BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_MISSING_DISCONNECT)
      .buffer(ProtocolBuffer.create().writeUUID(player.getUniqueId())
        .writeString(player.getName()).writeObject(createOwnNetworkServiceInfo()))
      .targetNodes()
      .build()
      .send();
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
    return new NetworkConnectionInfo(uniqueId, name, version, userAddress, listener, onlineMode, legacy,
      networkServiceInfo);
  }

  /**
   * @deprecated moved to {@link BridgeProxyHelper}
   */
  @Deprecated
  public static boolean isFallbackService(ServiceInfoSnapshot serviceInfoSnapshot) {
    return BridgeProxyHelper.isFallbackService(serviceInfoSnapshot);
  }

}
