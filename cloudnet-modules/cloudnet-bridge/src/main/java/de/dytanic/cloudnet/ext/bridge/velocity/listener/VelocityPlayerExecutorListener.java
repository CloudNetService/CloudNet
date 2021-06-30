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

package de.dytanic.cloudnet.ext.bridge.velocity.listener;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.api.proxy.server.ServerInfo;
import de.dytanic.cloudnet.ext.bridge.listener.PlayerExecutorListener;
import de.dytanic.cloudnet.ext.bridge.velocity.VelocityCloudNetHelper;
import java.util.Collection;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VelocityPlayerExecutorListener extends PlayerExecutorListener<Player> {

  private final ProxyServer proxyServer;

  public VelocityPlayerExecutorListener(ProxyServer proxyServer) {
    this.proxyServer = proxyServer;
  }

  @Nullable
  @Override
  protected Player getPlayer(@NotNull UUID uniqueId) {
    return this.proxyServer.getPlayer(uniqueId).orElse(null);
  }

  @Override
  protected @NotNull Collection<Player> getOnlinePlayers() {
    return this.proxyServer.getAllPlayers();
  }

  @Override
  protected void connect(@NotNull Player player, @NotNull String service) {
    this.proxyServer.getServer(service).ifPresent(server -> player.createConnectionRequest(server).fireAndForget());
  }

  @Override
  protected void kick(@NotNull Player player, @NotNull String reason) {
    player.disconnect(LegacyComponentSerializer.legacySection().deserialize(reason));
  }

  @Override
  protected void sendMessage(@NotNull Player player, @NotNull String message) {
    player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(message));
  }

  @Override
  protected void sendMessageComponent(@NotNull Player player, @NotNull String data) {
    player.sendMessage(GsonComponentSerializer.gson().deserialize(data));
  }

  @Override
  protected void sendPluginMessage(@NotNull Player player, @NotNull String tag, byte[] data) {
    ChannelIdentifier identifier = new LegacyChannelIdentifier(tag);
    VelocityCloudNetHelper.getProxyServer().getChannelRegistrar().register(identifier);
    player.sendPluginMessage(identifier, data);
  }

  private void broadcastMessage(@NotNull Component component, @Nullable String permission) {
    for (Player player : this.proxyServer.getAllPlayers()) {
      if (permission == null || player.hasPermission(permission)) {
        player.sendMessage(component);
      }
    }
  }

  @Override
  protected void broadcastMessageComponent(@NotNull String data, @Nullable String permission) {
    this.broadcastMessage(GsonComponentSerializer.gson().deserialize(data), permission);
  }

  @Override
  protected void broadcastMessage(@NotNull String message, @Nullable String permission) {
    this.broadcastMessage(LegacyComponentSerializer.legacySection().deserialize(message), permission);
  }

  @Override
  protected void connectToFallback(@NotNull Player player) {
    VelocityCloudNetHelper.connectToFallback(player,
      player.getCurrentServer().map(ServerConnection::getServerInfo).map(ServerInfo::getName).orElse(null));
  }

  @Override
  protected void dispatchCommand(@NotNull Player player, @NotNull String command) {
    this.proxyServer.getCommandManager().executeAsync(player, command);
  }
}
