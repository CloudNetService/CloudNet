/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.cloudperms.nukkit;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.plugin.PluginManager;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.driver.util.ModuleHelper;
import eu.cloudnetservice.ext.platforminject.api.PlatformEntrypoint;
import eu.cloudnetservice.ext.platforminject.api.stereotype.PlatformPlugin;
import eu.cloudnetservice.modules.cloudperms.PermissionsUpdateListener;
import eu.cloudnetservice.modules.cloudperms.nukkit.listener.NukkitCloudPermissionsPlayerListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
@PlatformPlugin(
  platform = "nukkit",
  name = "CloudNet-CloudPerms",
  authors = "CloudNetService",
  version = "@version@",
  api = "1.0.5",
  description = "Nukkit extension which implement the permission management system from CloudNet into Nukkit"
)
public final class NukkitCloudPermissionsPlugin implements PlatformEntrypoint {

  private final ModuleHelper moduleHelper;
  private final EventManager eventManager;
  private final PermissionManagement permissionManagement;

  private final Plugin plugin;
  private final Server server;
  private final PluginManager pluginManager;
  private final NukkitCloudPermissionsPlayerListener playerListener;

  @Inject
  public NukkitCloudPermissionsPlugin(
    @NonNull ModuleHelper moduleHelper,
    @NonNull EventManager eventManager,
    @NonNull PermissionManagement permissionManagement,
    @NonNull Plugin plugin,
    @NonNull Server server,
    @NonNull PluginManager pluginManager,
    @NonNull NukkitCloudPermissionsPlayerListener playerListener
  ) {
    this.moduleHelper = moduleHelper;
    this.eventManager = eventManager;
    this.permissionManagement = permissionManagement;
    this.plugin = plugin;
    this.server = server;
    this.pluginManager = pluginManager;
    this.playerListener = playerListener;
  }

  @Override
  public void onLoad() {
    this.pluginManager.registerEvents(this.playerListener, this.plugin);
    this.eventManager.registerListener(new PermissionsUpdateListener<>(
      runnable -> Server.getInstance().getScheduler().scheduleTask(this.plugin, runnable),
      Player::sendCommandData,
      Player::getUniqueId,
      uuid -> this.server.getPlayer(uuid).orElse(null),
      this.permissionManagement,
      () -> this.server.getOnlinePlayers().values()));
  }

  @Override
  public void onDisable() {
    this.moduleHelper.unregisterAll(this.getClass().getClassLoader());
  }
}
