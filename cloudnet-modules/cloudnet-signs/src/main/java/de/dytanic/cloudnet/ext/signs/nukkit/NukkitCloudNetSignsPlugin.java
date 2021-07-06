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

package de.dytanic.cloudnet.ext.signs.nukkit;

import cn.nukkit.plugin.PluginBase;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.signs.AbstractSignManagement;
import de.dytanic.cloudnet.ext.signs.configuration.entry.SignConfigurationEntry;
import de.dytanic.cloudnet.ext.signs.nukkit.command.CommandCloudSign;
import de.dytanic.cloudnet.ext.signs.nukkit.listener.NukkitSignInteractionListener;
import de.dytanic.cloudnet.wrapper.Wrapper;

public class NukkitCloudNetSignsPlugin extends PluginBase {

  private NukkitSignManagement signManagement;

  @Override
  public void onEnable() {
    this.signManagement = new NukkitSignManagement(this);
    CloudNetDriver.getInstance().getServicesRegistry()
      .registerService(AbstractSignManagement.class, "NukkitSignManagement", this.signManagement);

    this.initListeners();
  }

  @Override
  public void onDisable() {
    CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
    Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());

    CloudNetDriver.getInstance().getServicesRegistry()
      .unregisterService(AbstractSignManagement.class, this.signManagement);
  }

  private void initListeners() {
    //Commands
    super.getServer().getCommandMap().register("CloudNet-Signs", new CommandCloudSign(this.signManagement));

    //CloudNet listeners
    CloudNetDriver.getInstance().getEventManager().registerListener(this.signManagement);

    //Nukkit listeners
    super.getServer().getPluginManager().registerEvents(new NukkitSignInteractionListener(this.signManagement), this);

    //Sign knockback scheduler
    SignConfigurationEntry signConfigurationEntry = this.signManagement.getOwnSignConfigurationEntry();

    if (signConfigurationEntry != null && signConfigurationEntry.getKnockbackDistance() > 0
      && signConfigurationEntry.getKnockbackStrength() > 0) {
      super.getServer().getScheduler()
        .scheduleDelayedRepeatingTask(this, new NukkitSignKnockbackRunnable(this.signManagement), 20, 5);
    }
  }


}
