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

import static de.dytanic.cloudnet.ext.bridge.platform.helper.ProxyPlatformHelper.toCurrentNode;

import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.ext.bridge.BridgeManagement;
import de.dytanic.cloudnet.ext.bridge.player.NetworkServiceInfo;
import java.util.UUID;
import lombok.NonNull;

public final class ServerPlatformHelper {

  private ServerPlatformHelper() {
    throw new UnsupportedOperationException();
  }

  public static void sendChannelMessageLoginSuccess(@NonNull UUID playerUniqueId, @NonNull NetworkServiceInfo info) {
    toCurrentNode()
      .message("server_player_login")
      .channel(BridgeManagement.BRIDGE_PLAYER_CHANNEL_NAME)
      .buffer(DataBuf.empty().writeUniqueId(playerUniqueId).writeObject(info))
      .build()
      .send();
  }

  public static void sendChannelMessageDisconnected(@NonNull UUID playerUniqueId, @NonNull NetworkServiceInfo info) {
    toCurrentNode()
      .message("server_player_disconnect")
      .channel(BridgeManagement.BRIDGE_PLAYER_CHANNEL_NAME)
      .buffer(DataBuf.empty().writeUniqueId(playerUniqueId).writeObject(info))
      .build()
      .send();
  }
}
