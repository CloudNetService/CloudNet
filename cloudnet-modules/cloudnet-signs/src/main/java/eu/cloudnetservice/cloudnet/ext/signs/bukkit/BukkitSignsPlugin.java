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

package eu.cloudnetservice.cloudnet.ext.signs.bukkit;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.ext.signs.GlobalChannelMessageListener;
import eu.cloudnetservice.cloudnet.ext.signs.bukkit.functionality.CommandSigns;
import eu.cloudnetservice.cloudnet.ext.signs.bukkit.functionality.SignInteractListener;
import eu.cloudnetservice.cloudnet.ext.signs.service.ServiceSignManagement;
import eu.cloudnetservice.cloudnet.ext.signs.service.SignsServiceListener;
import org.bukkit.Bukkit;
import org.bukkit.block.Sign;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class BukkitSignsPlugin extends JavaPlugin {

  @Override
  public void onEnable() {
    ServiceSignManagement<Sign> signManagement = new BukkitSignManagement(this);
    signManagement.initialize();
    signManagement.registerToServiceRegistry();
    // bukkit command
    PluginCommand pluginCommand = this.getCommand("cloudsign");
    if (pluginCommand != null) {
      CommandSigns commandSigns = new CommandSigns(signManagement);
      pluginCommand.setExecutor(commandSigns);
      pluginCommand.setTabCompleter(commandSigns);
    }
    // bukkit listeners
    Bukkit.getPluginManager().registerEvents(new SignInteractListener(signManagement), this);
    // cloudnet listeners
    CloudNetDriver.getInstance().getEventManager().registerListeners(
      new GlobalChannelMessageListener(signManagement), new SignsServiceListener(signManagement));
  }

  @Override
  public void onDisable() {
    BukkitSignManagement.getDefaultInstance().unregisterFromServiceRegistry();
    CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
  }
}
