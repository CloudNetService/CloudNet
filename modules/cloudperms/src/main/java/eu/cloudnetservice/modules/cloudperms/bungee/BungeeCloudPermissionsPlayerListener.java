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

package eu.cloudnetservice.modules.cloudperms.bungee;

import eu.cloudnetservice.driver.permission.Permission;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.modules.cloudperms.CloudPermissionsHelper;
import lombok.NonNull;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PermissionCheckEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public final class BungeeCloudPermissionsPlayerListener implements Listener {

  private final PermissionManagement permissionsManagement;

  public BungeeCloudPermissionsPlayerListener(@NonNull PermissionManagement permissionsManagement) {
    this.permissionsManagement = permissionsManagement;
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void handle(@NonNull LoginEvent event) {
    CloudPermissionsHelper.initPermissionUser(
      this.permissionsManagement,
      event.getConnection().getUniqueId(),
      event.getConnection().getName(),
      message -> {
        event.setCancelled(true);
        event.setCancelReason(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', message)));
      },
      ProxyServer.getInstance().getConfig().isOnlineMode());
  }

  @EventHandler
  public void handle(@NonNull PermissionCheckEvent event) {
    var sender = event.getSender();
    if (sender instanceof ProxiedPlayer player) {
      var uniqueId = player.getUniqueId(); // must not be set ¯\_(ツ)_/¯
      if (uniqueId != null) {
        var permissionUser = this.permissionsManagement.user(uniqueId);
        if (permissionUser != null) {
          event.setHasPermission(
            this.permissionsManagement.hasPermission(permissionUser, Permission.of(event.getPermission())));
        }
      }
    }
  }

  @EventHandler
  public void handle(@NonNull PlayerDisconnectEvent event) {
    CloudPermissionsHelper.handlePlayerQuit(this.permissionsManagement, event.getPlayer().getUniqueId());
  }
}
