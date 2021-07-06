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

package de.dytanic.cloudnet.ext.bridge.velocity.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.ServerInfo;
import de.dytanic.cloudnet.ext.bridge.velocity.VelocityCloudNetHelper;
import de.dytanic.cloudnet.ext.bridge.velocity.util.VelocityComponentRenderer;

public final class CommandHub implements SimpleCommand {

  @Override
  public void execute(Invocation invocation) {
    if (!(invocation.source() instanceof Player)) {
      return;
    }

    Player player = (Player) invocation.source();
    if (VelocityCloudNetHelper.isOnMatchingFallbackInstance(player)) {
      player.sendMessage(VelocityComponentRenderer.rawTranslation("command-hub-already-in-hub"));
      return;
    }

    VelocityCloudNetHelper.connectToFallback(
      player,
      player.getCurrentServer().map(ServerConnection::getServerInfo).map(ServerInfo::getName).orElse(null)
    ).thenAccept(connectedFallback -> {
      if (connectedFallback != null) {
        player.sendMessage(VelocityComponentRenderer.rawTranslation("command-hub-success-connect",
          message -> message.replace("%server%", connectedFallback.getName())));
      } else {
        player.sendMessage(VelocityComponentRenderer.rawTranslation("command-hub-no-server-found"));
      }
    });
  }
}
