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

package de.dytanic.cloudnet.ext.chat;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class CloudNetChatPlugin extends JavaPlugin implements Listener {

  private String format;

  @Override
  public void onEnable() {
    this.getConfig().options().copyDefaults(true);
    this.saveConfig();

    this.format = this.getConfig().getString("format");

    this.getServer().getPluginManager().registerEvents(this, this);
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void handleChat(AsyncPlayerChatEvent event) {
    Player player = event.getPlayer();

    IPermissionUser user = CloudNetDriver.getInstance().getPermissionManagement().getUser(player.getUniqueId());

    if (user == null) {
      return;
    }

    IPermissionGroup group = CloudNetDriver.getInstance().getPermissionManagement().getHighestPermissionGroup(user);

    String message = event.getMessage().replace("%", "%%");
    if (player.hasPermission("cloudnet.chat.color")) {
      message = ChatColor.translateAlternateColorCodes('&', message);
    }

    if (ChatColor.stripColor(message).trim().isEmpty()) {
      event.setCancelled(true);
      return;
    }

    String format = this.format
      .replace("%name%", player.getName())
      .replace("%uniqueId%", player.getUniqueId().toString());

    if (group != null) {
      format = ChatColor.translateAlternateColorCodes('&',
        format
          .replace("%group%", group.getName())
          .replace("%display%", group.getDisplay())
          .replace("%prefix%", group.getPrefix())
          .replace("%suffix%", group.getSuffix())
          .replace("%color%", group.getColor())
      );
    } else {
      format = ChatColor.translateAlternateColorCodes('&',
        format
          .replace("%group%", "")
          .replace("%display%", "")
          .replace("%prefix%", "")
          .replace("%suffix%", "")
          .replace("%color%", "")
      );
    }

    event.setFormat(format.replace("%message%", message));
  }

}
