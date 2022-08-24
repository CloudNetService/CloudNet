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

package eu.cloudnetservice.modules.npc.platform.bukkit;

import eu.cloudnetservice.driver.util.ModuleUtil;
import eu.cloudnetservice.modules.npc.platform.bukkit.command.NPCCommand;
import eu.cloudnetservice.modules.npc.platform.bukkit.listener.BukkitEntityProtectionListener;
import eu.cloudnetservice.modules.npc.platform.bukkit.listener.BukkitFunctionalityListener;
import eu.cloudnetservice.modules.npc.platform.bukkit.listener.BukkitWorldListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class BukkitNPCPlugin extends JavaPlugin {

  @Override
  public void onEnable() {
    // init the npc management
    var management = new BukkitPlatformNPCManagement(this);
    management.registerToServiceRegistry();
    management.initialize();
    // register all listeners
    Bukkit.getPluginManager().registerEvents(new BukkitFunctionalityListener(management), this);
    Bukkit.getPluginManager().registerEvents(new BukkitEntityProtectionListener(management), this);
    Bukkit.getPluginManager().registerEvents(new BukkitWorldListener(this, management), this);
    // register the commands
    var command = this.getCommand("cn");
    if (command != null) {
      var executor = new NPCCommand(this, management);
      // set the executor
      command.setExecutor(executor);
      command.setTabCompleter(executor);
    }
  }

  @Override
  public void onDisable() {
    ModuleUtil.unregisterAll(this.getClass().getClassLoader());
  }
}
