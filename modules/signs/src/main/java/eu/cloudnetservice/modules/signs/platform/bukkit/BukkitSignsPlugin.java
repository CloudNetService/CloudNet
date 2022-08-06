/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.signs.platform.bukkit;

import eu.cloudnetservice.driver.CloudNetDriver;
import eu.cloudnetservice.driver.util.ModuleUtil;
import eu.cloudnetservice.modules.signs.SharedChannelMessageListener;
import eu.cloudnetservice.modules.signs.platform.SignsPlatformListener;
import eu.cloudnetservice.modules.signs.platform.bukkit.functionality.SignInteractListener;
import eu.cloudnetservice.modules.signs.platform.bukkit.functionality.SignsCommand;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class BukkitSignsPlugin extends JavaPlugin {

  @Override
  public void onEnable() {
    var signManagement = new BukkitSignManagement(this);
    signManagement.initialize();
    signManagement.registerToServiceRegistry();
    // bukkit command
    var pluginCommand = this.getCommand("cloudsign");
    if (pluginCommand != null) {
      var commandSigns = new SignsCommand(signManagement);
      pluginCommand.setExecutor(commandSigns);
      pluginCommand.setTabCompleter(commandSigns);
    }
    // bukkit listeners
    Bukkit.getPluginManager().registerEvents(new SignInteractListener(signManagement), this);
    // cloudnet listeners
    CloudNetDriver.instance().eventManager().registerListeners(
      new SharedChannelMessageListener(signManagement),
      new SignsPlatformListener(signManagement));
  }

  @Override
  public void onDisable() {
    ModuleUtil.unregisterAll(this.getClass().getClassLoader());
  }
}
