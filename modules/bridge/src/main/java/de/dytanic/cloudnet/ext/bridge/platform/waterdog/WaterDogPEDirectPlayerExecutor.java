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

package de.dytanic.cloudnet.ext.bridge.platform.waterdog;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.ext.bridge.platform.PlatformBridgeManagement;
import de.dytanic.cloudnet.ext.bridge.platform.PlatformPlayerExecutorAdapter;
import de.dytanic.cloudnet.ext.bridge.player.executor.ServerSelectorType;
import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import dev.waterdog.waterdogpe.utils.types.TextContainer;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class WaterDogPEDirectPlayerExecutor extends PlatformPlayerExecutorAdapter {

  private final UUID uniqueId;
  private final PlatformBridgeManagement<ProxiedPlayer, ?> management;
  private final Supplier<Collection<? extends ProxiedPlayer>> playerSupplier;

  public WaterDogPEDirectPlayerExecutor(
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
    var serverInfo = ProxyServer.getInstance().getServerInfo(serviceName);
    if (serverInfo != null) {
      this.playerSupplier.get().forEach(player -> player.connect(serverInfo));
    }
  }

  @Override
  public void connectSelecting(@NotNull ServerSelectorType selectorType) {
    this.management.getCachedServices().stream()
      .sorted(selectorType.getComparator())
      .map(service -> ProxyServer.getInstance().getServerInfo(service.getName()))
      .filter(Objects::nonNull)
      .findFirst()
      .ifPresent(server -> this.playerSupplier.get().forEach(player -> player.connect(server)));
  }

  @Override
  public void connectToFallback() {
    this.playerSupplier.get().stream()
      .map(player -> new Pair<>(player, this.management.getFallback(player)))
      .filter(pair -> pair.getSecond().isPresent())
      .map(p -> new Pair<>(p.getFirst(), ProxyServer.getInstance().getServerInfo(p.getSecond().get().getName())))
      .filter(pair -> pair.getSecond() != null)
      .forEach(pair -> pair.getFirst().connect(pair.getSecond()));
  }

  @Override
  public void connectToGroup(@NotNull String group, @NotNull ServerSelectorType selectorType) {
    this.management.getCachedServices().stream()
      .filter(service -> service.getConfiguration().getGroups().contains(group))
      .sorted(selectorType.getComparator())
      .map(service -> ProxyServer.getInstance().getServerInfo(service.getName()))
      .filter(Objects::nonNull)
      .forEach(server -> this.playerSupplier.get().forEach(player -> player.connect(server)));
  }

  @Override
  public void connectToTask(@NotNull String task, @NotNull ServerSelectorType selectorType) {
    this.management.getCachedServices().stream()
      .filter(service -> service.getServiceId().getTaskName().equals(task))
      .sorted(selectorType.getComparator())
      .map(service -> ProxyServer.getInstance().getServerInfo(service.getName()))
      .filter(Objects::nonNull)
      .forEach(server -> this.playerSupplier.get().forEach(player -> player.connect(server)));
  }

  @Override
  public void kick(@NotNull Component message) {
    this.playerSupplier.get()
      .forEach(player -> player.disconnect(new TextContainer(legacySection().serialize(message))));
  }

  @Override
  public void sendMessage(@NotNull Component message) {
    this.playerSupplier.get()
      .forEach(player -> player.sendMessage(new TextContainer(legacySection().serialize(message))));
  }

  @Override
  public void sendChatMessage(@NotNull Component message, @Nullable String permission) {
    this.playerSupplier.get().forEach(player -> {
      if (permission == null || player.hasPermission(permission)) {
        player.sendMessage(new TextContainer(legacySection().serialize(message)));
      }
    });
  }

  @Override
  public void sendPluginMessage(@NotNull String tag, byte[] data) {
    // no-op
  }

  @Override
  public void dispatchProxyCommand(@NotNull String command) {
    this.playerSupplier.get().forEach(player -> player.chat(command));
  }
}
