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

package de.dytanic.cloudnet.ext.bridge.platform.velocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.ext.bridge.platform.PlatformBridgeManagement;
import de.dytanic.cloudnet.ext.bridge.platform.PlatformPlayerExecutorAdapter;
import de.dytanic.cloudnet.ext.bridge.player.executor.ServerSelectorType;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.title.Title;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class VelocityDirectPlayerExecutor extends PlatformPlayerExecutorAdapter {

  private final UUID uniqueId;
  private final ProxyServer proxyServer;
  private final PlatformBridgeManagement<Player, ?> management;
  private final Supplier<Collection<? extends Player>> playerSupplier;

  public VelocityDirectPlayerExecutor(
    @NotNull UUID uniqueId,
    @NotNull ProxyServer proxyServer,
    @NotNull PlatformBridgeManagement<Player, ?> management,
    @NotNull Supplier<Collection<? extends Player>> playerSupplier
  ) {
    this.uniqueId = uniqueId;
    this.proxyServer = proxyServer;
    this.management = management;
    this.playerSupplier = playerSupplier;
  }

  @Override
  public @NotNull UUID getPlayerUniqueId() {
    return this.uniqueId;
  }

  @Override
  public void connect(@NotNull String serviceName) {
    this.proxyServer.getServer(serviceName).ifPresent(
      server -> this.playerSupplier.get().forEach(player -> player.createConnectionRequest(server).fireAndForget()));
  }

  @Override
  public void connectSelecting(@NotNull ServerSelectorType selectorType) {
    this.management.getCachedServices().stream()
      .sorted(selectorType.getComparator())
      .map(server -> this.proxyServer.getServer(server.getName()))
      .filter(Optional::isPresent)
      .map(Optional::get)
      .findFirst()
      .ifPresent(
        server -> this.playerSupplier.get().forEach(player -> player.createConnectionRequest(server).fireAndForget()));
  }

  @Override
  public void connectToFallback() {
    this.playerSupplier.get().stream()
      .map(player -> new Pair<>(player, this.management.getFallback(player)))
      .filter(pair -> pair.getSecond().isPresent())
      .map(pair -> new Pair<>(pair.getFirst(), this.proxyServer.getServer(pair.getSecond().get().getName())))
      .filter(pair -> pair.getSecond().isPresent())
      .forEach(pair -> pair.getFirst().createConnectionRequest(pair.getSecond().get()).fireAndForget());
  }

  @Override
  public void connectToGroup(@NotNull String group, @NotNull ServerSelectorType selectorType) {
    this.management.getCachedServices().stream()
      .filter(service -> service.getConfiguration().getGroups().contains(group))
      .sorted(selectorType.getComparator())
      .map(service -> this.proxyServer.getServer(service.getName()))
      .filter(Optional::isPresent)
      .map(Optional::get)
      .forEach(
        server -> this.playerSupplier.get().forEach(player -> player.createConnectionRequest(server).fireAndForget()));
  }

  @Override
  public void connectToTask(@NotNull String task, @NotNull ServerSelectorType selectorType) {
    this.management.getCachedServices().stream()
      .filter(service -> service.getServiceId().getTaskName().equals(task))
      .sorted(selectorType.getComparator())
      .map(service -> this.proxyServer.getServer(service.getName()))
      .filter(Optional::isPresent)
      .map(Optional::get)
      .forEach(
        server -> this.playerSupplier.get().forEach(player -> player.createConnectionRequest(server).fireAndForget()));
  }

  @Override
  public void kick(@NotNull TextComponent message) {
    this.playerSupplier.get().forEach(player -> player.disconnect(message));
  }

  @Override
  public void sendTitle(@NotNull Title title) {
    this.playerSupplier.get().forEach(player -> player.showTitle(title));
  }

  @Override
  public void sendMessage(@NotNull TextComponent message) {
    this.playerSupplier.get().forEach(player -> player.sendMessage(message));
  }

  @Override
  public void sendChatMessage(@NotNull TextComponent message, @Nullable String permission) {
    this.playerSupplier.get().forEach(player -> {
      if (permission == null || player.hasPermission(permission)) {
        player.sendMessage(message);
      }
    });
  }

  @Override
  public void sendPluginMessage(@NotNull String tag, byte[] data) {
    this.playerSupplier.get().forEach(player -> player.sendPluginMessage(MinecraftChannelIdentifier.from(tag), data));
  }

  @Override
  public void dispatchProxyCommand(@NotNull String command) {
    this.playerSupplier.get().forEach(player -> player.spoofChatInput(command));
  }
}
