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

package eu.cloudnetservice.cloudnet.ext.signs.platform.nukkit;

import cn.nukkit.Server;
import cn.nukkit.blockentity.BlockEntitySign;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.plugin.PluginBase;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.ext.signs.GlobalChannelMessageListener;
import eu.cloudnetservice.cloudnet.ext.signs.platform.PlatformSignManagement;
import eu.cloudnetservice.cloudnet.ext.signs.platform.SignsPlatformListener;
import eu.cloudnetservice.cloudnet.ext.signs.platform.nukkit.functionality.CommandSigns;
import eu.cloudnetservice.cloudnet.ext.signs.platform.nukkit.functionality.SignInteractListener;

public class NukkitSignsPlugin extends PluginBase {

  @Override
  public void onEnable() {
    PlatformSignManagement<BlockEntitySign> signManagement = new NukkitSignManagement(this);
    signManagement.initialize();
    signManagement.registerToServiceRegistry();
    // command
    PluginCommand<?> pluginCommand = (PluginCommand<?>) this.getCommand("cloudsign");
    if (pluginCommand != null) {
      pluginCommand.setExecutor(new CommandSigns(signManagement));
    }
    // nukkit listeners
    Server.getInstance().getPluginManager().registerEvents(new SignInteractListener(signManagement), this);
    // cloudnet listener
    CloudNetDriver.getInstance().getEventManager().registerListeners(
      new GlobalChannelMessageListener(signManagement), new SignsPlatformListener(signManagement));
  }

  @Override
  public void onDisable() {
    NukkitSignManagement.getDefaultInstance().unregisterFromServiceRegistry();
    CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
  }
}
