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

package eu.cloudnetservice.modules.signs.platform.nukkit;

import cn.nukkit.Server;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.plugin.PluginBase;
import eu.cloudnetservice.driver.CloudNetDriver;
import eu.cloudnetservice.driver.util.ModuleUtil;
import eu.cloudnetservice.modules.signs.SharedChannelMessageListener;
import eu.cloudnetservice.modules.signs.platform.SignsPlatformListener;
import eu.cloudnetservice.modules.signs.platform.nukkit.functionality.SignInteractListener;
import eu.cloudnetservice.modules.signs.platform.nukkit.functionality.SignsCommand;

public class NukkitSignsPlugin extends PluginBase {

  @Override
  public void onEnable() {
    var signManagement = new NukkitSignManagement(this);
    signManagement.initialize();
    signManagement.registerToServiceRegistry();
    // command
    var pluginCommand = (PluginCommand<?>) this.getCommand("cloudsign");
    if (pluginCommand != null) {
      pluginCommand.setExecutor(new SignsCommand(signManagement));
    }
    // nukkit listeners
    Server.getInstance().getPluginManager().registerEvents(new SignInteractListener(signManagement), this);
    // cloudnet listener
    CloudNetDriver.instance().eventManager().registerListeners(
      new SharedChannelMessageListener(signManagement),
      new SignsPlatformListener(signManagement));
  }

  @Override
  public void onDisable() {
    ModuleUtil.unregisterAll(this.getClass().getClassLoader());
  }
}
