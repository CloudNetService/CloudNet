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

package de.dytanic.cloudnet.ext.cloudperms.bukkit.listener;

import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsHelper;
import de.dytanic.cloudnet.ext.cloudperms.bukkit.BukkitPermissionInjectionHelper;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public final class BukkitCloudNetCloudPermissionsPlayerListener implements Listener {

  private final Plugin plugin;
  private final IPermissionManagement permissionsManagement;

  public BukkitCloudNetCloudPermissionsPlayerListener(Plugin plugin, IPermissionManagement permissionsManagement) {
    this.plugin = plugin;
    this.permissionsManagement = permissionsManagement;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void handlePreLogin(AsyncPlayerPreLoginEvent event) {
    if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
      return;
    }

    CloudPermissionsHelper
      .initPermissionUser(this.permissionsManagement, event.getUniqueId(), event.getName(), message -> {
        event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
        event.setKickMessage(ChatColor.translateAlternateColorCodes('&', message));
      }, Bukkit.getOnlineMode());
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void handle(PlayerLoginEvent event) {
    if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
      return;
    }

    try {
      BukkitPermissionInjectionHelper.injectPlayer(event.getPlayer());
    } catch (Throwable exception) {
      this.plugin.getLogger()
        .log(Level.SEVERE, "Error while injecting permissible for player " + event.getPlayer(), exception);
      event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
    }
  }

  @EventHandler
  public void handleQuit(PlayerQuitEvent event) {
    CloudPermissionsHelper.handlePlayerQuit(this.permissionsManagement, event.getPlayer().getUniqueId());
  }
}
