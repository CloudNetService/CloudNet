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

package eu.cloudnetservice.modules.cloudperms.bukkit.listener;

import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.modules.cloudperms.CloudPermissionsHelper;
import eu.cloudnetservice.modules.cloudperms.bukkit.BukkitPermissionHelper;
import eu.cloudnetservice.wrapper.configuration.WrapperConfiguration;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@Singleton
public final class BukkitCloudPermissionsPlayerListener implements Listener {

  private final WrapperConfiguration wrapperConfiguration;
  private final PermissionManagement permissionsManagement;

  @Inject
  public BukkitCloudPermissionsPlayerListener(
    @NonNull WrapperConfiguration wrapperConfiguration,
    @NonNull PermissionManagement permissionsManagement
  ) {
    this.wrapperConfiguration = wrapperConfiguration;
    this.permissionsManagement = permissionsManagement;
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void handlePreLogin(@NonNull AsyncPlayerPreLoginEvent event) {
    if (event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED) {
      CloudPermissionsHelper.initPermissionUser(
        this.permissionsManagement,
        event.getUniqueId(),
        event.getName(),
        message -> {
          event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
          event.setKickMessage(ChatColor.translateAlternateColorCodes('&', message));
        },
        Bukkit.getOnlineMode());
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void handle(@NonNull PlayerLoginEvent event) {
    if (event.getResult() == PlayerLoginEvent.Result.ALLOWED) {
      BukkitPermissionHelper.injectPlayer(event.getPlayer(), this.wrapperConfiguration, this.permissionsManagement);
    }
  }

  @EventHandler
  public void handleQuit(@NonNull PlayerQuitEvent event) {
    CloudPermissionsHelper.handlePlayerQuit(this.permissionsManagement, event.getPlayer().getUniqueId());
  }
}
