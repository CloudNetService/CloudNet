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

package eu.cloudnetservice.modules.cloudperms.nukkit.listener;

import cn.nukkit.Server;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerAsyncPreLoginEvent;
import cn.nukkit.event.player.PlayerLoginEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.modules.cloudperms.CloudPermissionsHelper;
import eu.cloudnetservice.modules.cloudperms.nukkit.NukkitPermissionInjectionHelper;
import eu.cloudnetservice.wrapper.configuration.WrapperConfiguration;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
public final class NukkitCloudPermissionsPlayerListener implements Listener {

  private final WrapperConfiguration wrapperConfiguration;
  private final PermissionManagement permissionsManagement;

  @Inject
  public NukkitCloudPermissionsPlayerListener(
    @NonNull WrapperConfiguration wrapperConfiguration,
    @NonNull PermissionManagement permissionsManagement
  ) {
    this.wrapperConfiguration = wrapperConfiguration;
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
      NukkitPermissionInjectionHelper.injectPermissible(
        event.getPlayer(),
        this.wrapperConfiguration,
        this.permissionsManagement);
    }
  }

  @EventHandler
  public void handle(@NonNull PlayerQuitEvent event) {
    CloudPermissionsHelper.handlePlayerQuit(this.permissionsManagement, event.getPlayer().getUniqueId());
  }
}
