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

package de.dytanic.cloudnet.ext.cloudperms.bungee;

import de.dytanic.cloudnet.driver.permission.Permission;
import de.dytanic.cloudnet.driver.permission.PermissionManagement;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsHelper;
import java.util.UUID;
import lombok.NonNull;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
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

  public BungeeCloudPermissionsPlayerListener(PermissionManagement permissionsManagement) {
    this.permissionsManagement = permissionsManagement;
  }

  @EventHandler(priority = EventPriority.LOW)
  public void handle(@NonNull LoginEvent event) {
    if (!event.isCancelled()) {
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
  }

  @EventHandler
  public void handle(@NonNull PermissionCheckEvent event) {
    CommandSender sender = event.getSender();
    if (sender instanceof ProxiedPlayer) {
      UUID uniqueId = ((ProxiedPlayer) sender).getUniqueId(); // must not be set ¯\_(ツ)_/¯
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
