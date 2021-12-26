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

package de.dytanic.cloudnet.ext.cloudperms.nukkit.listener;

import cn.nukkit.Server;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerAsyncPreLoginEvent;
import cn.nukkit.event.player.PlayerLoginEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import de.dytanic.cloudnet.driver.permission.PermissionManagement;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsHelper;
import de.dytanic.cloudnet.ext.cloudperms.nukkit.NukkitPermissionInjectionHelper;
import lombok.NonNull;

public final class NukkitCloudPermissionsPlayerListener implements Listener {

  private final PermissionManagement permissionsManagement;

  public NukkitCloudPermissionsPlayerListener(@NonNull PermissionManagement permissionsManagement) {
    this.permissionsManagement = permissionsManagement;
  }

  @EventHandler(priority = EventPriority.LOW)
  public void handle(@NonNull PlayerAsyncPreLoginEvent event) {
    if (event.getLoginResult() == PlayerAsyncPreLoginEvent.LoginResult.SUCCESS) {
      CloudPermissionsHelper.initPermissionUser(
        this.permissionsManagement,
        event.getUuid(),
        event.getName(),
        message -> event.disAllow(message.replace("&", "§")),
        Server.getInstance().getPropertyBoolean("xbox-auth", true));
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void handle(@NonNull PlayerLoginEvent event) {
    if (!event.isCancelled()) {
      NukkitPermissionInjectionHelper.injectPermissible(event.getPlayer(), this.permissionsManagement);
    }
  }

  @EventHandler
  public void handle(@NonNull PlayerQuitEvent event) {
    CloudPermissionsHelper.handlePlayerQuit(this.permissionsManagement, event.getPlayer().getUniqueId());
  }
}
