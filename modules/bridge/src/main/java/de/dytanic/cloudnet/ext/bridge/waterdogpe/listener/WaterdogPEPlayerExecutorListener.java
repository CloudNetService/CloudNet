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

package de.dytanic.cloudnet.ext.bridge.waterdogpe.listener;

import de.dytanic.cloudnet.ext.bridge.listener.PlayerExecutorListener;
import de.dytanic.cloudnet.ext.bridge.waterdogpe.WaterdogPECloudNetHelper;
import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import java.util.Collection;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WaterdogPEPlayerExecutorListener extends PlayerExecutorListener<ProxiedPlayer> {

  @Nullable
  @Override
  protected ProxiedPlayer getPlayer(@NotNull UUID uniqueId) {
    return ProxyServer.getInstance().getPlayer(uniqueId);
  }

  @Override
  protected @NotNull Collection<ProxiedPlayer> getOnlinePlayers() {
    return ProxyServer.getInstance().getPlayers().values();
  }

  @Override
  protected void connect(@NotNull ProxiedPlayer player, @NotNull String service) {
    ServerInfo serverInfo = ProxyServer.getInstance().getServerInfo(service);
    if (serverInfo != null) {
      player.connect(serverInfo);
    }
  }

  @Override
  protected void kick(@NotNull ProxiedPlayer player, @NotNull String reason) {
    player.disconnect(reason);
  }

  @Override
  protected void sendMessage(@NotNull ProxiedPlayer player, @NotNull String message) {
    player.sendMessage(message);
  }

  @Override
  protected void sendMessageComponent(@NotNull ProxiedPlayer player, @NotNull String data) {

  }

  @Override
  protected void sendPluginMessage(@NotNull ProxiedPlayer player, @NotNull String tag, byte[] data) {
  }

  @Override
  protected void broadcastMessageComponent(@NotNull String data, @Nullable String permission) {

  }

  @Override
  protected void broadcastMessage(@NotNull String message, @Nullable String permission) {
    for (ProxiedPlayer proxiedPlayer : ProxyServer.getInstance().getPlayers().values()) {
      if (permission == null || proxiedPlayer.hasPermission(permission)) {
        proxiedPlayer.sendMessage(message);
      }
    }
  }

  @Override
  protected void connectToFallback(@NotNull ProxiedPlayer player) {
    WaterdogPECloudNetHelper
      .connectToFallback(player, player.getServerInfo() != null ? player.getServerInfo().getServerName() : null);
  }

  @Override
  protected void dispatchCommand(@NotNull ProxiedPlayer player, @NotNull String command) {
    ProxyServer.getInstance().dispatchCommand(player, command);
  }

}
