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

package de.dytanic.cloudnet.ext.cloudperms.bukkit;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.cloudperms.bukkit.listener.BukkitCloudNetCloudPermissionsPlayerListener;
import de.dytanic.cloudnet.ext.cloudperms.bukkit.vault.VaultSupport;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

public final class BukkitCloudNetCloudPermissionsPlugin extends JavaPlugin {

  @Override
  public void onEnable() {
    this.checkForVault();
    Bukkit.getOnlinePlayers().forEach(this::injectCloudPermissible);

    this.getServer().getPluginManager().registerEvents(new BukkitCloudNetCloudPermissionsPlayerListener(
      this,
      CloudNetDriver.getInstance().getPermissionManagement()
    ), this);
  }

  @Override
  public void onDisable() {
    CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
    Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());
  }

  @Internal
  public void injectCloudPermissible(@NotNull Player player) {
    try {
      BukkitPermissionInjectionHelper.injectPlayer(player);
    } catch (Throwable exception) {
      this.getLogger().log(Level.SEVERE, "Exception while injecting cloud permissible", exception);
    }
  }

  private void checkForVault() {
    if (super.getServer().getPluginManager().isPluginEnabled("Vault")) {
      VaultSupport.hook(this, CloudNetDriver.getInstance().getPermissionManagement());
    }
  }
}
