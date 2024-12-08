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

package eu.cloudnetservice.modules.syncproxy.impl.platform.velocity;

import com.velocitypowered.api.network.ProtocolState;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.network.rpc.factory.RPCFactory;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.ext.component.ComponentFormats;
import eu.cloudnetservice.ext.platforminject.api.stereotype.ProvidesFor;
import eu.cloudnetservice.modules.syncproxy.SyncProxyManagement;
import eu.cloudnetservice.modules.syncproxy.impl.platform.PlatformSyncProxyManagement;
import eu.cloudnetservice.wrapper.configuration.WrapperConfiguration;
import eu.cloudnetservice.wrapper.holder.ServiceInfoHolder;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@Singleton
@ProvidesFor(platform = "velocity", types = {PlatformSyncProxyManagement.class, SyncProxyManagement.class})
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
    @NonNull @Named("taskScheduler") ScheduledExecutorService executorService
  ) {
    super(
      rpcFactory,
      eventManager,
      networkClient,
      wrapperConfig,
      serviceInfoHolder,
      serviceProvider,
      executorService);

    this.proxyServer = proxyServer;
    this.init();
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
  public void playerTabList(@NonNull Player player, @Nullable String header, @Nullable String footer) {
    if (player.getProtocolState() != ProtocolState.PLAY) {
      // prevent tab list updates if player is in an invalid state. Issue #1467
      return;
    }

    if (header == null || footer == null) {
      player.getTabList().clearHeaderAndFooter();
    } else {
      player.sendPlayerListHeaderAndFooter(
        ComponentFormats.BUNGEE_TO_ADVENTURE.convert(this.replaceTabPlaceholder(header, player)),
        ComponentFormats.BUNGEE_TO_ADVENTURE.convert(this.replaceTabPlaceholder(footer, player)));
    }
  }

  @Override
  public void disconnectPlayer(@NonNull Player player, @NonNull String message) {
    player.disconnect(ComponentFormats.BUNGEE_TO_ADVENTURE.convert(message));
  }

  @Override
  public void messagePlayer(@NonNull Player player, @Nullable String message) {
    if (message != null) {
      player.sendMessage(ComponentFormats.BUNGEE_TO_ADVENTURE.convert(message));
    }
  }

  @Override
  public boolean checkPlayerPermission(@NonNull Player player, @NonNull String permission) {
    return player.hasPermission(permission);
  }

  private @NonNull String replaceTabPlaceholder(@NonNull String input, @NonNull Player player) {
    var server = player.getCurrentServer()
      .map(serverConnection -> serverConnection.getServerInfo().getName())
      .orElse("UNAVAILABLE");

    return input
      .replace("%ping%", String.valueOf(player.getPing()))
      .replace("%server%", server);
  }
}
