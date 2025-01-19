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

package eu.cloudnetservice.modules.bridge.impl.platform.velocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import eu.cloudnetservice.modules.bridge.impl.platform.PlatformBridgeManagement;
import eu.cloudnetservice.modules.bridge.impl.platform.PlatformPlayerExecutorAdapter;
import eu.cloudnetservice.modules.bridge.player.executor.ServerSelectorType;
import io.vavr.Tuple2;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.jetbrains.annotations.Nullable;

final class VelocityDirectPlayerExecutor extends PlatformPlayerExecutorAdapter<Player> {

  private final ProxyServer proxyServer;
  private final PlatformBridgeManagement<Player, ?> management;

  public VelocityDirectPlayerExecutor(
    @NonNull UUID uniqueId,
    @NonNull ProxyServer proxyServer,
    @NonNull PlatformBridgeManagement<Player, ?> management,
    @NonNull Supplier<Collection<? extends Player>> playerSupplier
  ) {
    super(uniqueId, playerSupplier);

    this.proxyServer = proxyServer;
    this.management = management;
  }

  @Override
  public void connect(@NonNull String serviceName) {
    this.proxyServer.getServer(serviceName).ifPresent(
      server -> this.forEach(player -> player.createConnectionRequest(server).fireAndForget()));
  }

  @Override
  public void connectSelecting(@NonNull ServerSelectorType selectorType) {
    this.management.cachedServices().stream()
      .sorted(selectorType.comparator())
      .map(server -> this.proxyServer.getServer(server.name()))
      .filter(Optional::isPresent)
      .map(Optional::get)
      .findFirst()
      .ifPresent(server -> this.forEach(player -> player.createConnectionRequest(server).fireAndForget()));
  }

  @Override
  public void connectToFallback() {
    this.playerSupplier.get().stream()
      .filter(Objects::nonNull)
      .map(player -> new Tuple2<>(player, this.management.fallback(player)))
      .filter(pair -> pair._2().isPresent())
      .map(pair -> new Tuple2<>(pair._1(), this.proxyServer.getServer(pair._2().get().name())))
      .filter(pair -> pair._2().isPresent())
      .findFirst()
      .ifPresent(pair -> pair._1().createConnectionRequest(pair._2().get()).fireAndForget());
  }

  @Override
  public void connectToGroup(@NonNull String group, @NonNull ServerSelectorType selectorType) {
    this.management.cachedServices().stream()
      .filter(service -> service.configuration().groups().contains(group))
      .sorted(selectorType.comparator())
      .map(service -> this.proxyServer.getServer(service.name()))
      .filter(Optional::isPresent)
      .map(Optional::get)
      .findFirst()
      .ifPresent(server -> this.forEach(player -> player.createConnectionRequest(server).fireAndForget()));
  }

  @Override
  public void connectToTask(@NonNull String task, @NonNull ServerSelectorType selectorType) {
    this.management.cachedServices().stream()
      .filter(service -> service.serviceId().taskName().equals(task))
      .sorted(selectorType.comparator())
      .map(service -> this.proxyServer.getServer(service.name()))
      .filter(Optional::isPresent)
      .map(Optional::get)
      .findFirst()
      .ifPresent(server -> this.forEach(player -> player.createConnectionRequest(server).fireAndForget()));
  }

  @Override
  public void kick(@NonNull Component message) {
    this.forEach(player -> player.disconnect(message));
  }

  @Override
  public void sendTitle(@NonNull Title title) {
    this.forEach(player -> player.showTitle(title));
  }

  @Override
  public void sendChatMessage(@NonNull Component message, @Nullable String permission) {
    this.forEach(player -> {
      if (permission == null || player.hasPermission(permission)) {
        player.sendMessage(message);
      }
    });
  }

  @Override
  public void sendPluginMessage(@NonNull String key, byte[] data) {
    this.forEach(player -> player.sendPluginMessage(MinecraftChannelIdentifier.from(key), data));
  }

  @Override
  public void spoofCommandExecution(@NonNull String command, boolean redirectToServer) {
    this.forEach(player -> this.proxyServer.getCommandManager().executeAsync(player, command).thenAccept(success -> {
      if (!success && redirectToServer) {
        player.spoofChatInput('/' + command);
      }
    }));
  }
}
