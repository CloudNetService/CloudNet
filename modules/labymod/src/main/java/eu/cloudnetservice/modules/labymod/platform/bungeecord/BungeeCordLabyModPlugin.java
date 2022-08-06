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

package eu.cloudnetservice.modules.labymod.platform.bungeecord;

import eu.cloudnetservice.driver.util.ModuleUtil;
import eu.cloudnetservice.modules.labymod.platform.PlatformLabyModListener;
import eu.cloudnetservice.modules.labymod.platform.PlatformLabyModManagement;
import eu.cloudnetservice.wrapper.Wrapper;
import net.md_5.bungee.api.plugin.Plugin;

public class BungeeCordLabyModPlugin extends Plugin {

  @Override
  public void onEnable() {
    // init the labymod management
    var labyModManagement = new PlatformLabyModManagement();
    // register the plugin channel message listener
    this.getProxy().getPluginManager().registerListener(this, new BungeeCordLabyModListener(labyModManagement));
    // register the common cloudnet listener for channel messages
    Wrapper.instance().eventManager().registerListener(new PlatformLabyModListener(labyModManagement));

  }

  @Override
  public void onDisable() {
    // unregister all listeners for cloudnet events
    ModuleUtil.unregisterAll(this.getClass().getClassLoader());
  }
}
