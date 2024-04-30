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

package eu.cloudnetservice.modules.syncproxy.platform.bungee;

import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.network.rpc.RPCFactory;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.ext.platforminject.api.stereotype.ProvidesFor;
import eu.cloudnetservice.modules.bridge.platform.bungeecord.BungeeCordHelper;
import eu.cloudnetservice.modules.syncproxy.SyncProxyManagement;
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
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.jetbrains.annotations.Nullable;

@Singleton
@ProvidesFor(platform = "bungeecord", types = {PlatformSyncProxyManagement.class, SyncProxyManagement.class})
public final class BungeeCordSyncProxyManagement extends PlatformSyncProxyManagement<ProxiedPlayer> {

  private final ProxyServer proxyServer;
  private final BungeeCordHelper bungeeCordHelper;

  @Inject
  public BungeeCordSyncProxyManagement(
    @NonNull RPCFactory rpcFactory,
    @NonNull ProxyServer proxyServer,
    @NonNull EventManager eventManager,
    @NonNull NetworkClient networkClient,
    @NonNull BungeeCordHelper bungeeCordHelper,
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
    this.bungeeCordHelper = bungeeCordHelper;
    this.init();
  }

  @Override
  public void registerService(@NonNull ServiceRegistry registry) {
    registry.registerProvider(PlatformSyncProxyManagement.class, "BungeeCordSyncProxyManagement", this);
  }

  @Override
  public @NonNull Collection<ProxiedPlayer> onlinePlayers() {
    return this.proxyServer.getPlayers();
  }

  @Override
  public @NonNull String playerName(@NonNull ProxiedPlayer player) {
    return player.getName();
  }

  @Override
  public @NonNull UUID playerUniqueId(@NonNull ProxiedPlayer player) {
    return player.getUniqueId();
  }

  @Override
  public void playerTabList(@NonNull ProxiedPlayer player, @NonNull Map<String, String> placeholders, @Nullable String header, @Nullable String footer) {
    placeholders.put("ping", String.valueOf(player.getPing()));
    placeholders.put("server", player.getServer() == null ? "UNAVAILABLE" : player.getServer().getInfo().getName());

    player.setTabHeader(
      header != null ? BungeeComponentSerializer.get().serialize(
        MiniMessage.miniMessage().deserialize(
          header,
          placeholders.entrySet()
            .stream()
            .map((entry) -> Placeholder.unparsed(entry.getKey(), entry.getValue()))
            .toArray((size) -> new TagResolver[size])
        )
      ) : null,
      footer != null ? BungeeComponentSerializer.get().serialize(
        MiniMessage.miniMessage().deserialize(
          footer,
          placeholders.entrySet()
            .stream()
            .map((entry) -> Placeholder.unparsed(entry.getKey(), entry.getValue()))
            .toArray((size) -> new TagResolver[size])
        )
      ) : null);
  }

  @Override
  public void disconnectPlayer(@NonNull ProxiedPlayer player, @NonNull Component message) {
    player.disconnect(BungeeComponentSerializer.get().serialize(message));
  }

  @Override
  public void messagePlayer(@NonNull ProxiedPlayer player, @Nullable Component message) {
    if (message != null) {
      player.sendMessage(BungeeComponentSerializer.get().serialize(message));
    }
  }

  @Override
  public boolean checkPlayerPermission(@NonNull ProxiedPlayer player, @NonNull String permission) {
    return player.hasPermission(permission);
  }
}
