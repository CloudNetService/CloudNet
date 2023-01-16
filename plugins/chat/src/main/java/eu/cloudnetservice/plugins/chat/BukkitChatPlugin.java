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

package eu.cloudnetservice.plugins.chat;

import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.ext.platforminject.api.PlatformEntrypoint;
import eu.cloudnetservice.ext.platforminject.api.stereotype.Dependency;
import eu.cloudnetservice.ext.platforminject.api.stereotype.PlatformPlugin;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

@Singleton
@PlatformPlugin(
  platform = "bukkit",
  name = "CloudNet-Chat",
  authors = "CloudNetService",
  pluginFileNames = "plugin.yml",
  version = "{project.build.version}",
  description = "Brings chat prefixes and colored message support to all server platforms",
  dependencies = @Dependency(name = "CloudNet-CloudPerms")
)
public class BukkitChatPlugin implements PlatformEntrypoint, Listener {

  private final String format;

  private final Plugin plugin;
  private final PluginManager pluginManager;
  private final PermissionManagement permissionManagement;

  @Inject
  public BukkitChatPlugin(
    @NonNull JavaPlugin plugin,
    @NonNull PluginManager pluginManager,
    @NonNull PermissionManagement permissionManagement
  ) {
    this.plugin = plugin;
    this.pluginManager = pluginManager;
    this.permissionManagement = permissionManagement;

    // save the default config
    plugin.getConfig().options().copyDefaults(true);
    plugin.saveDefaultConfig();

    // load the config
    this.format = plugin.getConfig().getString("format");
  }

  @Override
  public void onLoad() {
    this.pluginManager.registerEvents(this, this.plugin);
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void handleChat(@NonNull AsyncPlayerChatEvent event) {
    var player = event.getPlayer();
    var formattedMessage = ChatFormatter.buildFormat(
      player.getUniqueId(),
      player.getName(),
      player.getDisplayName(),
      this.format,
      event.getMessage(),
      player::hasPermission,
      ChatColor::translateAlternateColorCodes,
      this.permissionManagement);

    if (formattedMessage == null) {
      event.setCancelled(true);
    } else {
      event.setFormat(formattedMessage);
    }
  }
}
