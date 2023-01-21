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

package eu.cloudnetservice.plugins.simplenametags.bukkit;

import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.ext.platforminject.api.PlatformEntrypoint;
import eu.cloudnetservice.ext.platforminject.api.stereotype.Dependency;
import eu.cloudnetservice.ext.platforminject.api.stereotype.PlatformPlugin;
import eu.cloudnetservice.plugins.simplenametags.SimpleNameTagsManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;

@Singleton
@PlatformPlugin(
  platform = "bukkit",
  authors = "CloudNetService",
  pluginFileNames = "plugin.yml",
  name = "CloudNet-SimpleNameTags",
  version = "{project.build.version}",
  dependencies = @Dependency(name = "CloudNet-CloudPerms"),
  description = "Adds prefix, suffix and display name support to all server platforms"
)
public final class BukkitSimpleNameTagsPlugin implements PlatformEntrypoint, Listener {

  private final Plugin plugin;
  private final PluginManager pluginManager;
  private final SimpleNameTagsManager<Player> nameTagsManager;

  @Inject
  public BukkitSimpleNameTagsPlugin(
    @NonNull Plugin plugin,
    @NonNull BukkitScheduler scheduler,
    @NonNull EventManager eventManager,
    @NonNull PluginManager pluginManager,
    @NonNull PermissionManagement permissionManagement
  ) {
    this.plugin = plugin;
    this.nameTagsManager = new BukkitSimpleNameTagsManager(
      runnable -> scheduler.runTask(plugin, runnable),
      eventManager,
      permissionManagement);
    this.pluginManager = pluginManager;
  }

  @Override
  public void onLoad() {
    this.pluginManager.registerEvents(this, this.plugin);
  }

  @EventHandler
  public void handle(@NonNull PlayerJoinEvent event) {
    this.nameTagsManager.updateNameTagsFor(event.getPlayer());
  }
}
