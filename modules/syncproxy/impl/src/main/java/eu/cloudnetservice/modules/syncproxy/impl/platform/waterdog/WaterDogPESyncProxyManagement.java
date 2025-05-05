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

package eu.cloudnetservice.modules.syncproxy.impl.platform.waterdog;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
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
@ProvidesFor(platform = "waterdogpe", types = {PlatformSyncProxyManagement.class, SyncProxyManagement.class})
public final class WaterDogPESyncProxyManagement extends PlatformSyncProxyManagement<ProxiedPlayer> {

  private final ProxyServer proxyServer;

  @Inject
  public WaterDogPESyncProxyManagement(
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
  public @NonNull Collection<ProxiedPlayer> onlinePlayers() {
    return this.proxyServer.getPlayers().values();
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
  public void playerTabList(@NonNull ProxiedPlayer player, @Nullable String header, @Nullable String footer) {
    // there is no support for header and footer
  }

  @Override
  public void disconnectPlayer(@NonNull ProxiedPlayer player, @NonNull String message) {
    player.disconnect(ComponentFormats.ADVENTURE_TO_BUNGEE.convertText(message));
  }

  @Override
  public void messagePlayer(@NonNull ProxiedPlayer player, @Nullable String message) {
    if (message != null) {
      player.sendMessage(ComponentFormats.ADVENTURE_TO_BUNGEE.convertText(message));
    }
  }

  @Override
  public boolean checkPlayerPermission(@NonNull ProxiedPlayer player, @NonNull String permission) {
    return player.hasPermission(permission);
  }
}
