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

package eu.cloudnetservice.modules.bridge.impl.platform.limboloohp;

import com.loohp.limbo.events.EventHandler;
import com.loohp.limbo.events.Listener;
import com.loohp.limbo.events.player.PlayerJoinEvent;
import com.loohp.limbo.events.player.PlayerQuitEvent;
import com.loohp.limbo.player.Player;
import com.loohp.limbo.plugins.LimboPlugin;
import com.loohp.limbo.scheduler.LimboScheduler;
import eu.cloudnetservice.modules.bridge.impl.platform.PlatformBridgeManagement;
import eu.cloudnetservice.modules.bridge.impl.platform.helper.ServerPlatformHelper;
import eu.cloudnetservice.modules.bridge.player.NetworkPlayerServerInfo;
import eu.cloudnetservice.wrapper.holder.ServiceInfoHolder;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Locale;
import lombok.NonNull;

@Singleton
public final class LimboLoohpPlayerManagementListener implements Listener {

  private final LimboPlugin plugin;
  private final LimboScheduler scheduler;
  private final ServiceInfoHolder serviceInfoHolder;
  private final ServerPlatformHelper serverPlatformHelper;
  private final PlatformBridgeManagement<Player, NetworkPlayerServerInfo> management;

  @Inject
  public LimboLoohpPlayerManagementListener(
    @NonNull LimboPlugin plugin,
    @NonNull LimboScheduler scheduler,
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
  public void handle(@NonNull PlayerJoinEvent event) {
    var task = this.management.selfTask();
    // check if the current task is present
    if (task != null) {
      Player player = event.getPlayer();
      // check if maintenance is activated
      if (task.maintenance() && !player.hasPermission("cloudnet.bridge.maintenance")) {
        this.management.configuration().handleMessage(
          Locale.US,
          "server-join-cancel-because-maintenance",
          player::disconnect);
        return;
      }
      // check if a custom permission is required to join
      var permission = task.propertyHolder().getString("requiredPermission");
      if (permission != null && !player.hasPermission(permission)) {
        this.management.configuration().handleMessage(
          Locale.US,
          "server-join-cancel-because-permission",
          player::disconnect);

        return;
      }
    }

    this.serverPlatformHelper.sendChannelMessageLoginSuccess(
      event.getPlayer().getUniqueId(),
      this.management.createPlayerInformation(event.getPlayer()));
    // update the service info in the next tick
    this.scheduler.runTask(this.plugin, this.serviceInfoHolder::publishServiceInfoUpdate);
  }

  @EventHandler
  public void handle(@NonNull PlayerQuitEvent event) {
    this.serverPlatformHelper.sendChannelMessageDisconnected(
      event.getPlayer().getUniqueId(),
      this.management.ownNetworkServiceInfo());
    // update the service info in the next tick
    this.scheduler.runTask(this.plugin, this.serviceInfoHolder::publishServiceInfoUpdate);
  }
}
