/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.bridge.platform.bungeecord;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;
import static net.md_5.bungee.api.chat.TextComponent.fromLegacyText;

import eu.cloudnetservice.common.collection.Pair;
import eu.cloudnetservice.modules.bridge.platform.PlatformBridgeManagement;
import eu.cloudnetservice.modules.bridge.platform.PlatformPlayerExecutorAdapter;
import eu.cloudnetservice.modules.bridge.player.executor.ServerSelectorType;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent.Reason;
import net.md_5.bungee.api.plugin.PluginManager;
import org.jetbrains.annotations.Nullable;

final class BungeeCordDirectPlayerExecutor extends PlatformPlayerExecutorAdapter<ProxiedPlayer> {

  private final ProxyServer proxyServer;
  private final PluginManager pluginManager;
  private final PlatformBridgeManagement<ProxiedPlayer, ?> management;

  public BungeeCordDirectPlayerExecutor(
    @NonNull ProxyServer proxyServer,
    @NonNull UUID uniqueId,
    @NonNull PlatformBridgeManagement<ProxiedPlayer, ?> management,
    @NonNull Supplier<Collection<? extends ProxiedPlayer>> playerSupplier
  ) {
    super(uniqueId, playerSupplier);
    this.proxyServer = proxyServer;
    this.pluginManager = proxyServer.getPluginManager();
    this.management = management;
  }

  @Override
  public void connect(@NonNull String serviceName) {
    var serverInfo = this.proxyServer.getServerInfo(serviceName);
    if (serverInfo != null) {
      this.forEach(player -> player.connect(serverInfo, Reason.PLUGIN));
    }
  }

  @Override
  public void connectSelecting(@NonNull ServerSelectorType selectorType) {
    this.management.cachedServices().stream()
      .sorted(selectorType.comparator())
      .map(service -> this.proxyServer.getServerInfo(service.name()))
      .filter(Objects::nonNull)
      .findFirst()
      .ifPresent(server -> this.forEach(player -> player.connect(server, Reason.PLUGIN)));
  }

  @Override
  public void connectToFallback() {
    this.playerSupplier.get().stream()
      .filter(Objects::nonNull)
      .map(player -> new Pair<>(player, this.management.fallback(player)))
      .filter(pair -> pair.second().isPresent())
      .map(p -> new Pair<>(p.first(), this.proxyServer.getServerInfo(p.second().get().name())))
      .filter(pair -> pair.second() != null)
      .findFirst()
      .ifPresent(pair -> pair.first().connect(pair.second(), Reason.PLUGIN));
  }

  @Override
  public void connectToGroup(@NonNull String group, @NonNull ServerSelectorType selectorType) {
    this.management.cachedServices().stream()
      .filter(service -> service.configuration().groups().contains(group))
      .sorted(selectorType.comparator())
      .map(service -> this.proxyServer.getServerInfo(service.name()))
      .filter(Objects::nonNull)
      .findFirst()
      .ifPresent(server -> this.forEach(player -> player.connect(server, Reason.PLUGIN)));
  }

  @Override
  public void connectToTask(@NonNull String task, @NonNull ServerSelectorType selectorType) {
    this.management.cachedServices().stream()
      .filter(service -> service.serviceId().taskName().equals(task))
      .sorted(selectorType.comparator())
      .map(service -> this.proxyServer.getServerInfo(service.name()))
      .filter(Objects::nonNull)
      .findFirst()
      .ifPresent(server -> this.forEach(player -> player.connect(server, Reason.PLUGIN)));
  }

  @Override
  public void kick(@NonNull Component message) {
    this.forEach(player -> player.disconnect(fromLegacyText(legacySection().serialize(message))));
  }

  @Override
  protected void sendTitle(@NonNull Component title, @NonNull Component subtitle, int fadeIn, int stay, int fadeOut) {
    this.forEach(player -> this.proxyServer.createTitle()
      .title(fromLegacyText(legacySection().serialize(title)))
      .subTitle(fromLegacyText(legacySection().serialize(subtitle)))
      .fadeIn(fadeIn)
      .stay(stay)
      .fadeOut(fadeOut)
      .send(player));
  }

  @Override
  public void sendChatMessage(@NonNull Component message, @Nullable String permission) {
    this.forEach(player -> {
      if (permission == null || player.hasPermission(permission)) {
        player.sendMessage(fromLegacyText(legacySection().serialize(message)));
      }
    });
  }

  @Override
  public void sendPluginMessage(@NonNull String key, byte[] data) {
    this.forEach(player -> player.sendData(key, data));
  }

  @Override
  public void spoofCommandExecution(@NonNull String command, boolean redirectToServer) {
    this.forEach(player -> {
      if (!this.pluginManager.dispatchCommand(player, command) && redirectToServer) {
        player.chat('/' + command);
      }
    });
  }
}
