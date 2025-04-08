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

package eu.cloudnetservice.modules.bridge.impl.platform.bungeecord;

import eu.cloudnetservice.modules.bridge.impl.platform.PlatformBridgeManagement;
import eu.cloudnetservice.modules.bridge.impl.platform.PlatformPlayerExecutorAdapter;
import eu.cloudnetservice.modules.bridge.player.executor.ServerSelectorType;
import io.vavr.Tuple2;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent.Reason;
import net.md_5.bungee.api.plugin.PluginManager;
import org.jetbrains.annotations.Nullable;

final class BungeeCordDirectPlayerExecutor extends PlatformPlayerExecutorAdapter<ProxiedPlayer> {

  // the first minecraft version to support hex colors
  // https://minecraft.fandom.com/wiki/Java_Edition_1.16
  private static final int PROTOCOL_VERSION_1_16 = 735;

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
      .map(player -> new Tuple2<>(player, this.management.fallback(player)))
      .filter(pair -> pair._2().isPresent())
      .map(p -> new Tuple2<>(p._1(), this.proxyServer.getServerInfo(p._2().get().name())))
      .filter(pair -> pair._2() != null)
      .findFirst()
      .ifPresent(pair -> pair._1().connect(pair._2(), Reason.PLUGIN));
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
    this.forEach(player -> player.disconnect(this.convertComponent(message, player)));
  }

  @Override
  protected void sendTitle(@NonNull Component title, @NonNull Component subtitle, int fadeIn, int stay, int fadeOut) {
    this.forEach(player -> this.proxyServer.createTitle()
      .title(this.convertComponent(title, player))
      .subTitle(this.convertComponent(subtitle, player))
      .fadeIn(fadeIn)
      .stay(stay)
      .fadeOut(fadeOut)
      .send(player));
  }

  @Override
  public void sendChatMessage(@NonNull Component message, @Nullable String permission) {
    this.forEach(player -> {
      if (permission == null || player.hasPermission(permission)) {
        player.sendMessage(this.convertComponent(message, player));
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

  private @NonNull BaseComponent[] convertComponent(@NonNull Component component, @NonNull ProxiedPlayer player) {
    // check if we have to use legacy colors because the client is on an old version
    if (player.getPendingConnection().getVersion() < PROTOCOL_VERSION_1_16) {
      return BungeeComponentSerializer.legacy().serialize(component);
    } else {
      return BungeeComponentSerializer.get().serialize(component);
    }
  }
}
