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

package eu.cloudnetservice.cloudnet.ext.syncproxy.platform.waterdog;

import de.dytanic.cloudnet.common.registry.IServicesRegistry;
import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import eu.cloudnetservice.cloudnet.ext.syncproxy.platform.PlatformSyncProxyManagement;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class WaterDogPESyncProxyManagement extends PlatformSyncProxyManagement<ProxiedPlayer> {

  private final ProxyServer proxyServer;

  public WaterDogPESyncProxyManagement(@NotNull ProxyServer proxyServer) {
    this.proxyServer = proxyServer;
    this.init();
  }

  @Override
  public void registerService(@NotNull IServicesRegistry registry) {
    registry.registerService(PlatformSyncProxyManagement.class, "WaterDogPESyncProxyManagement", this);
  }

  @Override
  public void unregisterService(@NotNull IServicesRegistry registry) {
    registry.unregisterService(PlatformSyncProxyManagement.class, "WaterDogPESyncProxyManagement");
  }

  @Override
  public void schedule(@NotNull Runnable runnable, long time, @NotNull TimeUnit unit) {
    this.proxyServer.getScheduler().scheduleDelayed(runnable, (int) (unit.toSeconds(time) / 20));
  }

  @Override
  public @NotNull Collection<ProxiedPlayer> onlinePlayers() {
    return this.proxyServer.getPlayers().values();
  }

  @Override
  public @NotNull String playerName(@NotNull ProxiedPlayer player) {
    return player.getName();
  }

  @Override
  public @NotNull UUID playerUniqueId(@NotNull ProxiedPlayer player) {
    return player.getUniqueId();
  }

  @Override
  public void playerTabList(@NotNull ProxiedPlayer player, @Nullable String header, @Nullable String footer) {
    // there is no support for header and footer
  }

  @Override
  public void disconnectPlayer(@NotNull ProxiedPlayer player, @NotNull String message) {
    player.sendMessage(message);
  }

  @Override
  public void messagePlayer(@NotNull ProxiedPlayer player, @Nullable String message) {
    if (message == null) {
      return;
    }
    player.sendMessage(message);
  }

  @Override
  public boolean checkPlayerPermission(@NotNull ProxiedPlayer player, @NotNull String permission) {
    return player.hasPermission(permission);
  }
}
