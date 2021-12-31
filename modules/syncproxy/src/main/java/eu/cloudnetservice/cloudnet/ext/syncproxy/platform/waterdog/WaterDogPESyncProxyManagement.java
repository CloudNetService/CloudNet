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

package eu.cloudnetservice.cloudnet.ext.syncproxy.platform.waterdog;

import de.dytanic.cloudnet.common.registry.ServicesRegistry;
import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import eu.cloudnetservice.cloudnet.ext.syncproxy.platform.PlatformSyncProxyManagement;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class WaterDogPESyncProxyManagement extends PlatformSyncProxyManagement<ProxiedPlayer> {

  private final ProxyServer proxyServer;

  public WaterDogPESyncProxyManagement(@NonNull ProxyServer proxyServer) {
    this.proxyServer = proxyServer;
    this.init();
  }

  @Override
  public void registerService(@NonNull ServicesRegistry registry) {
    registry.registerService(PlatformSyncProxyManagement.class, "WaterDogPESyncProxyManagement", this);
  }

  @Override
  public void unregisterService(@NonNull ServicesRegistry registry) {
    registry.unregisterService(PlatformSyncProxyManagement.class, "WaterDogPESyncProxyManagement");
  }

  @Override
  public void schedule(@NonNull Runnable runnable, long time, @NonNull TimeUnit unit) {
    this.proxyServer.getScheduler().scheduleDelayed(runnable, (int) (unit.toSeconds(time) / 20));
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
    player.sendMessage(message);
  }

  @Override
  public void messagePlayer(@NonNull ProxiedPlayer player, @Nullable String message) {
    if (message == null) {
      return;
    }
    player.sendMessage(message);
  }

  @Override
  public boolean checkPlayerPermission(@NonNull ProxiedPlayer player, @NonNull String permission) {
    return player.hasPermission(permission);
  }
}
