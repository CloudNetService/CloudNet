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

package eu.cloudnetservice.modules.bridge.platform.nukkit;

import cn.nukkit.Server;
import cn.nukkit.plugin.PluginBase;
import eu.cloudnetservice.cloudnet.wrapper.Wrapper;

public final class NukkitBridgePlugin extends PluginBase {

  @Override
  public void onEnable() {
    var management = new NukkitBridgeManagement(Wrapper.instance());
    management.registerServices(Wrapper.instance().serviceRegistry());
    management.postInit();
    // register the listener
    Server.getInstance().getPluginManager().registerEvents(new NukkitPlayerManagementListener(this, management), this);
  }

  @Override
  public void onDisable() {
    Server.getInstance().getScheduler().cancelTask(this);
  }
}
