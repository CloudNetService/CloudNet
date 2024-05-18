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

package eu.cloudnetservice.modules.bridge.platform.nukkit;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerLoginEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.scheduler.ServerScheduler;
import eu.cloudnetservice.modules.bridge.platform.PlatformBridgeManagement;
import eu.cloudnetservice.modules.bridge.platform.helper.ServerPlatformHelper;
import eu.cloudnetservice.modules.bridge.player.NetworkPlayerServerInfo;
import eu.cloudnetservice.wrapper.holder.ServiceInfoHolder;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
public final class NukkitPlayerManagementListener implements Listener {

  private final Plugin plugin;
  private final ServerScheduler scheduler;
  private final ServiceInfoHolder serviceInfoHolder;
  private final ServerPlatformHelper serverPlatformHelper;
  private final PlatformBridgeManagement<Player, NetworkPlayerServerInfo> management;

  @Inject
  public NukkitPlayerManagementListener(
    @NonNull Plugin plugin,
    @NonNull ServerScheduler scheduler,
    @NonNull ServiceInfoHolder serviceInfoHolder,
    @NonNull ServerPlatformHelper serverPlatformHelper,
    @NonNull PlatformBridgeManagement<Player, NetworkPlayerServerInfo> management
  ) {
    this.plugin = plugin;
    this.scheduler = scheduler;
    this.serviceInfoHolder = serviceInfoHolder;
    this.serverPlatformHelper = serverPlatformHelper;
    this.management = management;
  }

  @EventHandler
  public void handle(@NonNull PlayerLoginEvent event) {
    var task = this.management.selfTask();
    // check if the current task is present
    if (task != null) {
      // check if maintenance is activated
      if (task.maintenance() && !event.getPlayer().hasPermission("cloudnet.bridge.maintenance")) {
        event.setCancelled(true);
        this.management.configuration().handleMessage(
          event.getPlayer().getLocale(),
          "server-join-cancel-because-maintenance",
          event::setKickMessage);
        return;
      }
      // check if a custom permission is required to join
      var permission = task.propertyHolder().getString("requiredPermission");
      if (permission != null && !event.getPlayer().hasPermission(permission)) {
        event.setCancelled(true);
        this.management.configuration().handleMessage(
          event.getPlayer().getLocale(),
          "server-join-cancel-because-permission",
          event::setKickMessage);
      }
    }
  }

  @EventHandler
  public void handle(@NonNull PlayerJoinEvent event) {
    this.serverPlatformHelper.sendChannelMessageLoginSuccess(
      event.getPlayer().getUniqueId(),
      this.management.createPlayerInformation(event.getPlayer()));
    // update the service info in the next tick
    this.scheduler.scheduleTask(this.plugin, this.serviceInfoHolder::publishServiceInfoUpdate);
  }

  @EventHandler
  public void handle(@NonNull PlayerQuitEvent event) {
    this.serverPlatformHelper.sendChannelMessageDisconnected(
      event.getPlayer().getUniqueId(),
      this.management.ownNetworkServiceInfo());
    // update the service info in the next tick
    this.scheduler.scheduleTask(this.plugin, this.serviceInfoHolder::publishServiceInfoUpdate);
  }
}
