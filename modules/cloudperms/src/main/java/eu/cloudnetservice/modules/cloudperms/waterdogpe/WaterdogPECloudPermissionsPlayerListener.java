/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.cloudperms.waterdogpe;

import dev.waterdog.waterdogpe.event.EventManager;
import dev.waterdog.waterdogpe.event.EventPriority;
import dev.waterdog.waterdogpe.event.defaults.PlayerDisconnectEvent;
import dev.waterdog.waterdogpe.event.defaults.PlayerLoginEvent;
import dev.waterdog.waterdogpe.event.defaults.PlayerPermissionCheckEvent;
import dev.waterdog.waterdogpe.utils.ConfigurationManager;
import eu.cloudnetservice.driver.permission.Permission;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.modules.cloudperms.CloudPermissionsHelper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
final class WaterdogPECloudPermissionsPlayerListener {

  @Inject
  public WaterdogPECloudPermissionsPlayerListener(
    @NonNull EventManager eventManager,
    @NonNull ConfigurationManager configurationManager,
    @NonNull PermissionManagement permissionManagement
  ) {
    eventManager.subscribe(
      PlayerLoginEvent.class,
      event -> CloudPermissionsHelper.initPermissionUser(
        permissionManagement,
        event.getPlayer().getUniqueId(),
        event.getPlayer().getName(),
        message -> {
          event.setCancelled(true);
          event.setCancelReason(message.replace('&', '§'));
        },
        configurationManager.getProxyConfig().isOnlineMode()
      ),
      EventPriority.LOW);

    eventManager.subscribe(PlayerPermissionCheckEvent.class, event -> {
      var permissionUser = permissionManagement.user(event.getPlayer().getUniqueId());
      if (permissionUser != null) {
        event.setHasPermission(
          permissionManagement.hasPermission(permissionUser, Permission.of(event.getPermission())));
      }
    });

    eventManager.subscribe(
      PlayerDisconnectEvent.class,
      event -> CloudPermissionsHelper.handlePlayerQuit(permissionManagement, event.getPlayer().getUniqueId()));
  }
}
