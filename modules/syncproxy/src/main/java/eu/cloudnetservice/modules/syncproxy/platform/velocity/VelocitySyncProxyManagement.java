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

package eu.cloudnetservice.modules.syncproxy.platform.velocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.network.rpc.RPCFactory;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.ext.component.ComponentFormats;
import eu.cloudnetservice.modules.syncproxy.platform.PlatformSyncProxyManagement;
import eu.cloudnetservice.wrapper.configuration.WrapperConfiguration;
import eu.cloudnetservice.wrapper.holder.ServiceInfoHolder;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.Nullable;

@Singleton
public final class VelocitySyncProxyManagement extends PlatformSyncProxyManagement<Player> {

  private final ProxyServer proxyServer;

  @Inject
  public VelocitySyncProxyManagement(
    @NonNull RPCFactory rpcFactory,
    @NonNull ProxyServer proxyServer,
    @NonNull EventManager eventManager,
    @NonNull NetworkClient networkClient,
    @NonNull WrapperConfiguration wrapperConfig,
    @NonNull ServiceInfoHolder serviceInfoHolder,
    @NonNull CloudServiceProvider serviceProvider,
    @NonNull @Named("taskScheduler") ScheduledExecutorService executorService,
    @NonNull PermissionManagement permissionManagement
  ) {
    super(
      rpcFactory,
      eventManager,
      networkClient,
      wrapperConfig,
      serviceInfoHolder,
      serviceProvider,
      executorService,
      permissionManagement);

    this.proxyServer = proxyServer;
    this.init();
  }

  @Override
  public void registerService(@NonNull ServiceRegistry registry) {
    registry.registerProvider(PlatformSyncProxyManagement.class, "VelocitySyncProxyManagement", this);
  }

  @Override
  public @NonNull Collection<Player> onlinePlayers() {
    return this.proxyServer.getAllPlayers();
  }

  @Override
  public @NonNull String playerName(@NonNull Player player) {
    return player.getUsername();
  }

  @Override
  public @NonNull UUID playerUniqueId(@NonNull Player player) {
    return player.getUniqueId();
  }

  @Override
  public void playerTabList(@NonNull Player player, @NonNull Map<String, String> placeholders, @Nullable String header, @Nullable String footer) {
    if (header == null || footer == null) {
      player.getTabList().clearHeaderAndFooter();
    } else {
      placeholders.put("server", player.getCurrentServer()
        .map(serverConnection -> serverConnection.getServerInfo().getName())
        .orElse("UNAVAILABLE"));
      placeholders.put("ping", String.valueOf(player.getPing()));

      player.sendPlayerListHeaderAndFooter(
        MiniMessage.miniMessage().deserialize(
          header,
          placeholders.entrySet()
            .stream()
            .map((entry) -> Placeholder.unparsed(entry.getKey(), entry.getValue()))
            .toArray((size) -> new TagResolver[size])
        ),
        MiniMessage.miniMessage().deserialize(
          footer,
          placeholders.entrySet()
            .stream()
            .map((entry) -> Placeholder.unparsed(entry.getKey(), entry.getValue()))
            .toArray((size) -> new TagResolver[size])
        ));
    }
  }

  @Override
  public void disconnectPlayer(@NonNull Player player, @NonNull Component message) {
    player.disconnect(message);
  }

  @Override
  public void messagePlayer(@NonNull Player player, @Nullable Component message) {
    if (message != null) {
      player.sendMessage(message);
    }
  }

  @Override
  public boolean checkPlayerPermission(@NonNull Player player, @NonNull String permission) {
    return player.hasPermission(permission);
  }
}
