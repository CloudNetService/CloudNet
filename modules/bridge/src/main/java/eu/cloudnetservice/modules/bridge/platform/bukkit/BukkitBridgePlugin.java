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

package eu.cloudnetservice.modules.bridge.platform.bukkit;

import eu.cloudnetservice.cloudnet.wrapper.Wrapper;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class BukkitBridgePlugin extends JavaPlugin {

  @Override
  public void onEnable() {
    // init the bridge management
    var management = new BukkitBridgeManagement(this);
    management.registerServices(Wrapper.instance().serviceRegistry());
    management.postInit();
    // register the bukkit listener
    Bukkit.getPluginManager().registerEvents(new BukkitPlayerManagementListener(this, management), this);
  }

  @Override
  public void onDisable() {
    Bukkit.getScheduler().cancelTasks(this);
  }
}
