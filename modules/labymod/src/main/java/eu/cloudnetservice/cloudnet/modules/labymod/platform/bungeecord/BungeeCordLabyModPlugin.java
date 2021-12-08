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

package eu.cloudnetservice.cloudnet.modules.labymod.platform.bungeecord;

import de.dytanic.cloudnet.wrapper.Wrapper;
import eu.cloudnetservice.cloudnet.modules.labymod.platform.PlatformLabyModListener;
import eu.cloudnetservice.cloudnet.modules.labymod.platform.PlatformLabyModManagement;
import net.md_5.bungee.api.plugin.Plugin;

public class BungeeCordLabyModPlugin extends Plugin {

  @Override
  public void onEnable() {
    // init the labymod management
    PlatformLabyModManagement labyModManagement = new PlatformLabyModManagement();
    // register the plugin channel message listener
    this.getProxy().getPluginManager().registerListener(this, new BungeeCordLabyModListener(labyModManagement));
    // register the common cloudnet listener for channel messages
    Wrapper.getInstance().getEventManager().registerListener(new PlatformLabyModListener(labyModManagement));

  }

  @Override
  public void onDisable() {
    // unregister all listeners for cloudnet events
    Wrapper.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
    Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());
  }
}
