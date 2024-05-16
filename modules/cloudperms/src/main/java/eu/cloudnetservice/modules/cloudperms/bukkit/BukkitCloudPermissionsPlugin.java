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

package eu.cloudnetservice.modules.cloudperms.bukkit;

import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.driver.util.ModuleHelper;
import eu.cloudnetservice.ext.platforminject.api.PlatformEntrypoint;
import eu.cloudnetservice.ext.platforminject.api.stereotype.PlatformPlugin;
import eu.cloudnetservice.modules.cloudperms.PermissionsUpdateListener;
import eu.cloudnetservice.modules.cloudperms.bukkit.listener.BukkitCloudPermissionsPlayerListener;
import eu.cloudnetservice.modules.cloudperms.bukkit.vault.VaultSupport;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

@Singleton
@PlatformPlugin(
  platform = "bukkit",
  name = "CloudNet-CloudPerms",
  authors = "CloudNetService",
  version = "@version@",
  description = "Bukkit extension which implement the permission management system from CloudNet into Bukkit"
)
public final class BukkitCloudPermissionsPlugin implements PlatformEntrypoint {

  private final JavaPlugin plugin;
  private final PluginManager pluginManager;
  private final BukkitCloudPermissionsPlayerListener playerListener;

  private final ModuleHelper moduleHelper;
  private final EventManager eventManager;
  private final PermissionManagement permissionManagement;

  @Inject
  public BukkitCloudPermissionsPlugin(
    @NonNull JavaPlugin plugin,
    @NonNull PluginManager pluginManager,
    @NonNull BukkitCloudPermissionsPlayerListener playerListener,
    @NonNull ModuleHelper moduleHelper,
    @NonNull EventManager eventManager,
    @NonNull PermissionManagement permissionManagement
  ) {
    this.plugin = plugin;
    this.pluginManager = pluginManager;
    this.playerListener = playerListener;
    this.moduleHelper = moduleHelper;
    this.eventManager = eventManager;
    this.permissionManagement = permissionManagement;
  }

  @Override
  public void onLoad() {
    // check if vault is loaded & register the permission inject listener
    this.checkForVault();
    this.pluginManager.registerEvents(this.playerListener, this.plugin);

    // register the update listener if the server can update the command tree to the player
    if (BukkitPermissionHelper.canUpdateCommandTree()) {
      this.eventManager.registerListener(new PermissionsUpdateListener<>(
        runnable -> Bukkit.getScheduler().runTask(this.plugin, runnable),
        BukkitPermissionHelper::resendCommandTree,
        Player::getUniqueId,
        Bukkit::getPlayer,
        this.permissionManagement,
        Bukkit::getOnlinePlayers));
    }
  }

  @Override
  public void onDisable() {
    this.moduleHelper.unregisterAll(this.getClass().getClassLoader());
  }

  private void checkForVault() {
    if (this.pluginManager.isPluginEnabled("Vault")) {
      VaultSupport.hook(this.plugin, this.permissionManagement);
    }
  }
}
