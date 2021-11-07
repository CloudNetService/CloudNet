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

package de.dytanic.cloudnet.ext.bridge.platform.bungeecord;

import static net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText;
import static net.md_5.bungee.api.chat.TextComponent.fromLegacyText;

import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.ext.bridge.platform.PlatformBridgeManagement;
import de.dytanic.cloudnet.ext.bridge.platform.PlatformPlayerExecutorAdapter;
import de.dytanic.cloudnet.ext.bridge.player.executor.ServerSelectorType;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent.Reason;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class BungeeCordDirectPlayerExecutor extends PlatformPlayerExecutorAdapter {

  private final UUID uniqueId;
  private final PlatformBridgeManagement<ProxiedPlayer, ?> management;
  private final Supplier<Collection<? extends ProxiedPlayer>> playerSupplier;

  public BungeeCordDirectPlayerExecutor(
    @NotNull UUID uniqueId,
    @NotNull PlatformBridgeManagement<ProxiedPlayer, ?> management,
    @NotNull Supplier<Collection<? extends ProxiedPlayer>> playerSupplier
  ) {
    this.uniqueId = uniqueId;
    this.management = management;
    this.playerSupplier = playerSupplier;
  }

  @Override
  public @NotNull UUID getPlayerUniqueId() {
    return this.uniqueId;
  }

  @Override
  public void connect(@NotNull String serviceName) {
    ServerInfo serverInfo = ProxyServer.getInstance().getServerInfo(serviceName);
    if (serverInfo != null) {
      this.playerSupplier.get().forEach(player -> player.connect(serverInfo, Reason.PLUGIN));
    }
  }

  @Override
  public void connectSelecting(@NotNull ServerSelectorType selectorType) {
    this.management.getCachedServices().stream()
      .sorted(selectorType.getComparator())
      .map(service -> ProxyServer.getInstance().getServerInfo(service.getName()))
      .filter(Objects::nonNull)
      .findFirst()
      .ifPresent(server -> this.playerSupplier.get().forEach(player -> player.connect(server, Reason.PLUGIN)));
  }

  @Override
  public void connectToFallback() {
    this.playerSupplier.get().stream()
      .map(player -> new Pair<>(player, this.management.getFallback(player)))
      .filter(pair -> pair.getSecond().isPresent())
      .map(p -> new Pair<>(p.getFirst(), ProxyServer.getInstance().getServerInfo(p.getSecond().get().getName())))
      .filter(pair -> pair.getSecond() != null)
      .forEach(pair -> pair.getFirst().connect(pair.getSecond(), Reason.PLUGIN));
  }

  @Override
  public void connectToGroup(@NotNull String group, @NotNull ServerSelectorType selectorType) {
    this.management.getCachedServices().stream()
      .filter(service -> service.getConfiguration().getGroups().contains(group))
      .sorted(selectorType.getComparator())
      .map(service -> ProxyServer.getInstance().getServerInfo(service.getName()))
      .filter(Objects::nonNull)
      .forEach(server -> this.playerSupplier.get().forEach(player -> player.connect(server, Reason.PLUGIN)));
  }

  @Override
  public void connectToTask(@NotNull String task, @NotNull ServerSelectorType selectorType) {
    this.management.getCachedServices().stream()
      .filter(service -> service.getServiceId().getTaskName().equals(task))
      .sorted(selectorType.getComparator())
      .map(service -> ProxyServer.getInstance().getServerInfo(service.getName()))
      .filter(Objects::nonNull)
      .forEach(server -> this.playerSupplier.get().forEach(player -> player.connect(server, Reason.PLUGIN)));
  }

  @Override
  public void kick(@NotNull TextComponent message) {
    this.playerSupplier.get().forEach(player -> player.disconnect(fromLegacyText(plainText().serialize(message))));
  }

  @Override
  protected void sendTitle(@NotNull Component title, @NotNull Component subtitle, int fadeIn, int stay, int fadeOut) {
    this.playerSupplier.get().forEach(player -> ProxyServer.getInstance().createTitle()
      .title(fromLegacyText(plainText().serialize(title)))
      .subTitle(fromLegacyText(plainText().serialize(subtitle)))
      .fadeIn(fadeIn)
      .stay(stay)
      .fadeOut(fadeOut)
      .send(player));
  }

  @Override
  public void sendMessage(@NotNull TextComponent message) {
    this.playerSupplier.get().forEach(player -> player.sendMessage(fromLegacyText(plainText().serialize(message))));
  }

  @Override
  public void sendChatMessage(@NotNull TextComponent message, @Nullable String permission) {
    this.playerSupplier.get().forEach(player -> {
      if (permission == null || player.hasPermission(permission)) {
        player.sendMessage(fromLegacyText(plainText().serialize(message)));
      }
    });
  }

  @Override
  public void sendPluginMessage(@NotNull String tag, byte[] data) {
    this.playerSupplier.get().forEach(player -> player.sendData(tag, data));
  }

  @Override
  public void dispatchProxyCommand(@NotNull String command) {
    this.playerSupplier.get().forEach(player -> player.chat(command));
  }
}
