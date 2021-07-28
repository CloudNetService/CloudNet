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

package de.dytanic.cloudnet.ext.bridge.waterdogpe;

import de.dytanic.cloudnet.ext.bridge.proxy.BridgeProxyHelper;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import dev.waterdog.waterdogpe.utils.types.IJoinHandler;
import dev.waterdog.waterdogpe.utils.types.IReconnectHandler;

public class WaterdogPECloudNetReconnectHandler implements IJoinHandler, IReconnectHandler {

  @Override
  public ServerInfo determineServer(ProxiedPlayer player) {
    return WaterdogPECloudNetHelper.getNextFallback(player, null).orElse(null);
  }

  @Override
  public ServerInfo getFallbackServer(ProxiedPlayer player, ServerInfo oldServer, String kickMessage) {
    BridgeProxyHelper.handleConnectionFailed(player.getUniqueId(), oldServer.getServerName());

    return WaterdogPECloudNetHelper.getNextFallback(player, oldServer).map(serverInfo -> {
      player.sendMessage(kickMessage);
      return serverInfo;
    }).orElse(null);
  }

}
