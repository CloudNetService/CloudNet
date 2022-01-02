/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.bridge.platform.waterdog;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import dev.waterdog.waterdogpe.utils.types.TextContainer;
import eu.cloudnetservice.cloudnet.common.collection.Pair;
import eu.cloudnetservice.modules.bridge.platform.PlatformBridgeManagement;
import eu.cloudnetservice.modules.bridge.platform.PlatformPlayerExecutorAdapter;
import eu.cloudnetservice.modules.bridge.player.executor.ServerSelectorType;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

final class WaterDogPEDirectPlayerExecutor extends PlatformPlayerExecutorAdapter {

  private final UUID uniqueId;
  private final PlatformBridgeManagement<ProxiedPlayer, ?> management;
  private final Supplier<Collection<? extends ProxiedPlayer>> playerSupplier;

  public WaterDogPEDirectPlayerExecutor(
    @NonNull UUID uniqueId,
    @NonNull PlatformBridgeManagement<ProxiedPlayer, ?> management,
    @NonNull Supplier<Collection<? extends ProxiedPlayer>> playerSupplier
  ) {
    this.uniqueId = uniqueId;
    this.management = management;
    this.playerSupplier = playerSupplier;
  }

  @Override
  public @NonNull UUID uniqueId() {
    return this.uniqueId;
  }

  @Override
  public void connect(@NonNull String serviceName) {
    var serverInfo = ProxyServer.getInstance().getServerInfo(serviceName);
    if (serverInfo != null) {
      this.playerSupplier.get().forEach(player -> player.connect(serverInfo));
    }
  }

  @Override
  public void connectSelecting(@NonNull ServerSelectorType selectorType) {
    this.management.cachedServices().stream()
      .sorted(selectorType.comparator())
      .map(service -> ProxyServer.getInstance().getServerInfo(service.name()))
      .filter(Objects::nonNull)
      .findFirst()
      .ifPresent(server -> this.playerSupplier.get().forEach(player -> player.connect(server)));
  }

  @Override
  public void connectToFallback() {
    this.playerSupplier.get().stream()
      .map(player -> new Pair<>(player, this.management.fallback(player)))
      .filter(pair -> pair.second().isPresent())
      .map(p -> new Pair<>(p.first(), ProxyServer.getInstance().getServerInfo(p.second().get().name())))
      .filter(pair -> pair.second() != null)
      .forEach(pair -> pair.first().connect(pair.second()));
  }

  @Override
  public void connectToGroup(@NonNull String group, @NonNull ServerSelectorType selectorType) {
    this.management.cachedServices().stream()
      .filter(service -> service.configuration().groups().contains(group))
      .sorted(selectorType.comparator())
      .map(service -> ProxyServer.getInstance().getServerInfo(service.name()))
      .filter(Objects::nonNull)
      .forEach(server -> this.playerSupplier.get().forEach(player -> player.connect(server)));
  }

  @Override
  public void connectToTask(@NonNull String task, @NonNull ServerSelectorType selectorType) {
    this.management.cachedServices().stream()
      .filter(service -> service.serviceId().taskName().equals(task))
      .sorted(selectorType.comparator())
      .map(service -> ProxyServer.getInstance().getServerInfo(service.name()))
      .filter(Objects::nonNull)
      .forEach(server -> this.playerSupplier.get().forEach(player -> player.connect(server)));
  }

  @Override
  public void kick(@NonNull Component message) {
    this.playerSupplier.get()
      .forEach(player -> player.disconnect(new TextContainer(legacySection().serialize(message))));
  }

  @Override
  public void sendMessage(@NonNull Component message) {
    this.playerSupplier.get()
      .forEach(player -> player.sendMessage(new TextContainer(legacySection().serialize(message))));
  }

  @Override
  public void sendChatMessage(@NonNull Component message, @Nullable String permission) {
    this.playerSupplier.get().forEach(player -> {
      if (permission == null || player.hasPermission(permission)) {
        player.sendMessage(new TextContainer(legacySection().serialize(message)));
      }
    });
  }

  @Override
  public void sendPluginMessage(@NonNull String tag, byte[] data) {
    // no-op
  }

  @Override
  public void dispatchProxyCommand(@NonNull String command) {
    this.playerSupplier.get().forEach(player -> player.chat(command));
  }
}
