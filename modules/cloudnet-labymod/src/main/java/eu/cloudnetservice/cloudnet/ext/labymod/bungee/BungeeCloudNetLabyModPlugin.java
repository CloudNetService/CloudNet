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

package eu.cloudnetservice.cloudnet.ext.labymod.bungee;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.ext.labymod.AbstractLabyModManagement;
import eu.cloudnetservice.cloudnet.ext.labymod.LabyModConstants;
import eu.cloudnetservice.cloudnet.ext.labymod.LabyModUtils;
import eu.cloudnetservice.cloudnet.ext.labymod.bungee.listener.BungeeLabyModListener;
import eu.cloudnetservice.cloudnet.ext.labymod.config.LabyModConfiguration;
import net.md_5.bungee.api.plugin.Plugin;

public class BungeeCloudNetLabyModPlugin extends Plugin {

  private AbstractLabyModManagement labyModManagement;

  @Override
  public void onEnable() {
    LabyModConfiguration configuration = LabyModUtils.getConfiguration();
    if (configuration == null || !configuration.isEnabled()) {
      return;
    }
    this.getProxy().registerChannel(LabyModConstants.LMC_CHANNEL_NAME);

    this.labyModManagement = new BungeeLabyModManagement(super.getProxy());

    this.getProxy().getPluginManager().registerListener(this, new BungeeLabyModListener(this.labyModManagement));
  }

  @Override
  public void onDisable() {
    CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
  }
}
