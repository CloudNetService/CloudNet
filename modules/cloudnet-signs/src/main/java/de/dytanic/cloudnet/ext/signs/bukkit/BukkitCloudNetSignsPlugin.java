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

package de.dytanic.cloudnet.ext.signs.bukkit;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.signs.AbstractSignManagement;
import de.dytanic.cloudnet.ext.signs.bukkit.command.CommandCloudSign;
import de.dytanic.cloudnet.ext.signs.bukkit.listener.BukkitSignInteractionListener;
import de.dytanic.cloudnet.ext.signs.configuration.entry.SignConfigurationEntry;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class BukkitCloudNetSignsPlugin extends JavaPlugin {

  private BukkitSignManagement signManagement;

  @Override
  public void onEnable() {
    this.signManagement = new BukkitSignManagement(this);
    CloudNetDriver.getInstance().getServicesRegistry()
      .registerService(AbstractSignManagement.class, "BukkitSignManagement", this.signManagement);

    this.initListeners();
  }

  @Override
  public void onDisable() {
    CloudNetDriver.getInstance().getEventManager().unregisterListeners(super.getClassLoader());
    Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());

    CloudNetDriver.getInstance().getServicesRegistry()
      .unregisterService(AbstractSignManagement.class, "BukkitSignManagement");
  }

  private void initListeners() {
    //Commands
    PluginCommand cloudSignCommand = this.getCommand("cloudsign");

    if (cloudSignCommand != null) {
      cloudSignCommand.setExecutor(new CommandCloudSign(this.signManagement));
      cloudSignCommand.setPermission("cloudnet.command.cloudsign");
      cloudSignCommand.setUsage("/cloudsign create <targetGroup>");
      cloudSignCommand.setDescription("Add or Removes signs from the provided Group configuration");
    }

    //CloudNet listeners
    CloudNetDriver.getInstance().getEventManager().registerListener(this.signManagement);

    //Bukkit listeners
    Bukkit.getPluginManager().registerEvents(new BukkitSignInteractionListener(this.signManagement), this);

    //Sign knockback scheduler
    SignConfigurationEntry signConfigurationEntry = this.signManagement.getOwnSignConfigurationEntry();

    if (signConfigurationEntry != null && signConfigurationEntry.getKnockbackDistance() > 0
      && signConfigurationEntry.getKnockbackStrength() > 0) {
      Bukkit.getScheduler()
        .scheduleSyncRepeatingTask(this, new BukkitSignKnockbackRunnable(this.signManagement), 20, 5);
    }
  }

}
