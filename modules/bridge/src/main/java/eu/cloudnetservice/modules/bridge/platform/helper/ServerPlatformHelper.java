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

package eu.cloudnetservice.modules.bridge.platform.helper;

import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.modules.bridge.BridgeManagement;
import eu.cloudnetservice.modules.bridge.player.NetworkPlayerServerInfo;
import eu.cloudnetservice.modules.bridge.player.NetworkServiceInfo;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.UUID;
import lombok.NonNull;

@Singleton
public final class ServerPlatformHelper {

  private final ProxyPlatformHelper proxyPlatformHelper;

  @Inject
  public ServerPlatformHelper(@NonNull ProxyPlatformHelper proxyPlatformHelper) {
    this.proxyPlatformHelper = proxyPlatformHelper;
  }

  public void sendChannelMessageLoginSuccess(
    @NonNull UUID playerUniqueId,
    @NonNull NetworkPlayerServerInfo info
  ) {
    this.proxyPlatformHelper.toCurrentNode()
      .message("server_player_login")
      .channel(BridgeManagement.BRIDGE_PLAYER_CHANNEL_NAME)
      .buffer(DataBuf.empty().writeUniqueId(playerUniqueId).writeObject(info))
      .build()
      .send();
  }

  public void sendChannelMessageDisconnected(@NonNull UUID playerUniqueId, @NonNull NetworkServiceInfo info) {
    this.proxyPlatformHelper.toCurrentNode()
      .message("server_player_disconnect")
      .channel(BridgeManagement.BRIDGE_PLAYER_CHANNEL_NAME)
      .buffer(DataBuf.empty().writeUniqueId(playerUniqueId).writeObject(info))
      .build()
      .send();
  }
}
