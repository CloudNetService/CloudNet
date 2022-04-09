/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.bridge.platform.helper;

import eu.cloudnetservice.cloudnet.driver.channel.ChannelMessage;
import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBuf;
import eu.cloudnetservice.cloudnet.wrapper.Wrapper;
import eu.cloudnetservice.modules.bridge.BridgeManagement;
import eu.cloudnetservice.modules.bridge.node.event.LocalPlayerPreLoginEvent.Result;
import eu.cloudnetservice.modules.bridge.player.NetworkPlayerProxyInfo;
import eu.cloudnetservice.modules.bridge.player.NetworkServiceInfo;
import java.util.UUID;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

@Internal
public final class ProxyPlatformHelper {

  private ProxyPlatformHelper() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull Result sendChannelMessagePreLogin(@NonNull NetworkPlayerProxyInfo playerInfo) {
    var result = toCurrentNode()
      .message("proxy_player_pre_login")
      .channel(BridgeManagement.BRIDGE_PLAYER_CHANNEL_NAME)
      .buffer(DataBuf.empty().writeObject(playerInfo))
      .build()
      .sendSingleQuery();
    return result == null ? Result.allowed() : result.content().readObject(Result.class);
  }

  public static void sendChannelMessageLoginSuccess(
    @NonNull NetworkPlayerProxyInfo proxyInfo,
    @Nullable NetworkServiceInfo joinServiceInfo
  ) {
    toCurrentNode()
      .message("proxy_player_login")
      .channel(BridgeManagement.BRIDGE_PLAYER_CHANNEL_NAME)
      .buffer(DataBuf.empty().writeObject(proxyInfo).writeObject(joinServiceInfo))
      .build()
      .send();
  }

  public static void sendChannelMessageServiceSwitch(@NonNull UUID playerId, @NonNull NetworkServiceInfo target) {
    toCurrentNode()
      .message("proxy_player_service_switch")
      .channel(BridgeManagement.BRIDGE_PLAYER_CHANNEL_NAME)
      .buffer(DataBuf.empty().writeUniqueId(playerId).writeObject(target))
      .build()
      .send();
  }

  public static void sendChannelMessageDisconnected(@NonNull UUID playerUniqueId) {
    toCurrentNode()
      .message("proxy_player_disconnect")
      .channel(BridgeManagement.BRIDGE_PLAYER_CHANNEL_NAME)
      .buffer(DataBuf.empty().writeUniqueId(playerUniqueId))
      .build()
      .send();
  }

  static @NonNull ChannelMessage.Builder toCurrentNode() {
    return ChannelMessage.builder().targetNode(Wrapper.instance().nodeUniqueId());
  }
}
