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

package de.dytanic.cloudnet.ext.cloudperms.waterdogpe.listener;

import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsHelper;
import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.event.EventManager;
import dev.waterdog.waterdogpe.event.EventPriority;
import dev.waterdog.waterdogpe.event.defaults.PlayerDisconnectEvent;
import dev.waterdog.waterdogpe.event.defaults.PlayerLoginEvent;
import dev.waterdog.waterdogpe.event.defaults.PlayerPermissionCheckEvent;

public class WaterdogPECloudNetCloudPermissionsPlayerListener {

  public WaterdogPECloudNetCloudPermissionsPlayerListener(IPermissionManagement permissionManagement) {
    EventManager eventManager = ProxyServer.getInstance().getEventManager();

    eventManager.subscribe(PlayerLoginEvent.class, event -> {
      if (!event.isCancelled()) {
        CloudPermissionsHelper.initPermissionUser(
          permissionManagement,
          event.getPlayer().getUniqueId(),
          event.getPlayer().getName(),
          message -> {
            event.setCancelled(true);
            event.setCancelReason(message.replace('&', '§'));
          }
        );
      }
    }, EventPriority.LOW);

    eventManager.subscribe(PlayerPermissionCheckEvent.class, event -> {
      IPermissionUser permissionUser = permissionManagement.getUser(event.getPlayer().getUniqueId());
      if (permissionUser != null) {
        event.setHasPermission(permissionManagement.hasPermission(permissionUser, event.getPermission()));
      }
    });

    eventManager.subscribe(PlayerDisconnectEvent.class,
      event -> CloudPermissionsHelper.handlePlayerQuit(permissionManagement, event.getPlayer().getUniqueId()));
  }
}
