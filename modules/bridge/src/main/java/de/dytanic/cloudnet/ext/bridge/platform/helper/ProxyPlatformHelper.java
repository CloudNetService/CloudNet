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

package de.dytanic.cloudnet.ext.bridge.platform.helper;

import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.ext.bridge.BridgeManagement;
import de.dytanic.cloudnet.ext.bridge.node.event.LocalPlayerPreLoginEvent.Result;
import de.dytanic.cloudnet.ext.bridge.player.NetworkPlayerProxyInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkServiceInfo;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.util.UUID;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus.Internal;

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

  public static void sendChannelMessageLoginSuccess(@NonNull NetworkPlayerProxyInfo info) {
    toCurrentNode()
      .message("proxy_player_login")
      .channel(BridgeManagement.BRIDGE_PLAYER_CHANNEL_NAME)
      .buffer(DataBuf.empty().writeObject(info))
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
