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

package eu.cloudnetservice.modules.bridge.platform.waterdog;

import static eu.cloudnetservice.ext.component.ComponentFormats.ADVENTURE_TO_BUNGEE;
import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
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
import org.jetbrains.annotations.Nullable;

final class WaterDogPEDirectPlayerExecutor extends PlatformPlayerExecutorAdapter<ProxiedPlayer> {

  private final ProxyServer proxyServer;
  private final PlatformBridgeManagement<ProxiedPlayer, ?> management;

  public WaterDogPEDirectPlayerExecutor(
    @NonNull UUID uniqueId,
    @NonNull ProxyServer proxyServer,
    @NonNull PlatformBridgeManagement<ProxiedPlayer, ?> management,
    @NonNull Supplier<Collection<? extends ProxiedPlayer>> playerSupplier
  ) {
    super(uniqueId, playerSupplier);
    this.management = management;
    this.proxyServer = proxyServer;
  }

  @Override
  public void connect(@NonNull String serviceName) {
    var serverInfo = this.proxyServer.getServerInfo(serviceName);
    if (serverInfo != null) {
      this.forEach(player -> player.connect(serverInfo));
    }
  }

  @Override
  public void connectSelecting(@NonNull ServerSelectorType selectorType) {
    this.management.cachedServices().stream()
      .sorted(selectorType.comparator())
      .map(service -> this.proxyServer.getServerInfo(service.name()))
      .filter(Objects::nonNull)
      .findFirst()
      .ifPresent(server -> this.forEach(player -> player.connect(server)));
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
      .ifPresent(pair -> pair.first().connect(pair.second()));
  }

  @Override
  public void connectToGroup(@NonNull String group, @NonNull ServerSelectorType selectorType) {
    this.management.cachedServices().stream()
      .filter(service -> service.configuration().groups().contains(group))
      .sorted(selectorType.comparator())
      .map(service -> this.proxyServer.getServerInfo(service.name()))
      .filter(Objects::nonNull)
      .findFirst()
      .ifPresent(server -> this.forEach(player -> player.connect(server)));
  }

  @Override
  public void connectToTask(@NonNull String task, @NonNull ServerSelectorType selectorType) {
    this.management.cachedServices().stream()
      .filter(service -> service.serviceId().taskName().equals(task))
      .sorted(selectorType.comparator())
      .map(service -> this.proxyServer.getServerInfo(service.name()))
      .filter(Objects::nonNull)
      .findFirst()
      .ifPresent(server -> this.forEach(player -> player.connect(server)));
  }

  @Override
  public void kick(@NonNull Component message) {
    this.forEach(player -> player.disconnect(ADVENTURE_TO_BUNGEE.convertText(legacySection().serialize(message))));
  }

  @Override
  public void sendChatMessage(@NonNull Component message, @Nullable String permission) {
    this.forEach(player -> {
      if (permission == null || player.hasPermission(permission)) {
        player.sendMessage(ADVENTURE_TO_BUNGEE.convertText(legacySection().serialize(message)));
      }
    });
  }

  @Override
  public void sendPluginMessage(@NonNull String key, byte[] data) {
    // no-op
  }

  @Override
  public void spoofCommandExecution(@NonNull String command, boolean redirectToServer) {
    this.forEach(player -> {
      if (!this.proxyServer.dispatchCommand(player, command) && redirectToServer) {
        player.chat('/' + command);
      }
    });
  }
}
