/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.bridge.impl.platform.helper;

import eu.cloudnetservice.driver.ComponentInfo;
import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.modules.bridge.BridgeManagement;
import eu.cloudnetservice.modules.bridge.node.event.LocalPlayerPreLoginEvent;
import eu.cloudnetservice.modules.bridge.player.NetworkPlayerProxyInfo;
import eu.cloudnetservice.modules.bridge.player.NetworkServiceInfo;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.UUID;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@Singleton
@ApiStatus.Internal
public final class ProxyPlatformHelper {

  private final ComponentInfo componentInfo;

  @Inject
  public ProxyPlatformHelper(@NonNull ComponentInfo componentInfo) {
    this.componentInfo = componentInfo;
  }

  public @NonNull LocalPlayerPreLoginEvent.Result sendChannelMessagePreLogin(
    @NonNull NetworkPlayerProxyInfo playerInfo
  ) {
    var result = this.toCurrentNode()
      .message("proxy_player_pre_login")
      .channel(BridgeManagement.BRIDGE_PLAYER_CHANNEL_NAME)
      .buffer(DataBuf.empty().writeObject(playerInfo))
      .build()
      .sendSingleQuery();
    return result == null
      ? LocalPlayerPreLoginEvent.Result.allowed()
      : result.content().readObject(LocalPlayerPreLoginEvent.Result.class);
  }

  public void sendChannelMessageLoginSuccess(
    @NonNull NetworkPlayerProxyInfo proxyInfo,
    @Nullable NetworkServiceInfo joinServiceInfo
  ) {
    this.toCurrentNode()
      .message("proxy_player_login")
      .channel(BridgeManagement.BRIDGE_PLAYER_CHANNEL_NAME)
      .buffer(DataBuf.empty().writeObject(proxyInfo).writeObject(joinServiceInfo))
      .build()
      .send();
  }

  public void sendChannelMessageServiceSwitch(@NonNull UUID playerId, @NonNull NetworkServiceInfo target) {
    this.toCurrentNode()
      .message("proxy_player_service_switch")
      .channel(BridgeManagement.BRIDGE_PLAYER_CHANNEL_NAME)
      .buffer(DataBuf.empty().writeUniqueId(playerId).writeObject(target))
      .build()
      .send();
  }

  public void sendChannelMessageDisconnected(@NonNull UUID playerUniqueId) {
    this.toCurrentNode()
      .message("proxy_player_disconnect")
      .channel(BridgeManagement.BRIDGE_PLAYER_CHANNEL_NAME)
      .buffer(DataBuf.empty().writeUniqueId(playerUniqueId))
      .build()
      .send();
  }

  @NonNull ChannelMessage.Builder toCurrentNode() {
    return ChannelMessage.builder().targetNode(this.componentInfo.nodeUniqueId());
  }
}
