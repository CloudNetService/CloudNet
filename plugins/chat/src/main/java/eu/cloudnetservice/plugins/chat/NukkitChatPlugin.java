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

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerChatEvent;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.plugin.PluginManager;
import cn.nukkit.utils.TextFormat;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.ext.platforminject.api.PlatformEntrypoint;
import eu.cloudnetservice.ext.platforminject.api.stereotype.Dependency;
import eu.cloudnetservice.ext.platforminject.api.stereotype.PlatformPlugin;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
@PlatformPlugin(
  platform = "nukkit",
  name = "CloudNet-Chat",
  authors = "CloudNetService",
  version = "{project.build.version}",
  description = "Brings chat prefixes and colored message support to all server platforms",
  dependencies = @Dependency(name = "CloudNet-CloudPerms")
)
public class NukkitChatPlugin implements PlatformEntrypoint, Listener {

  private final String format;
  private final Plugin plugin;
  private final PluginManager pluginManager;
  private final PermissionManagement permissionManagement;

  @Inject
  public NukkitChatPlugin(
    @NonNull PluginBase plugin,
    @NonNull PluginManager pluginManager,
    @NonNull PermissionManagement permissionManagement
  ) {
    this.plugin = plugin;
    this.pluginManager = pluginManager;
    this.permissionManagement = permissionManagement;

    // load the config
    plugin.saveDefaultConfig();
    this.format = plugin.getConfig().getString("format", "%display%%name% &8:&f %message%");
  }

  @Override
  public void onLoad() {
    this.pluginManager.registerEvents(this, this.plugin);
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void handle(@NonNull PlayerChatEvent event) {
    var player = event.getPlayer();
    var format = ChatFormatter.buildFormat(
      player.getUniqueId(),
      player.getName(),
      player.getDisplayName(),
      this.format,
      event.getMessage(),
      event.getPlayer()::hasPermission,
      TextFormat::colorize,
      this.permissionManagement);
    if (format == null) {
      event.setCancelled(true);
    } else {
      event.setFormat(format);
    }
  }
}
