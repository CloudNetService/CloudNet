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

package eu.cloudnetservice.modules.signs.platform.minestom;

import eu.cloudnetservice.driver.CloudNetDriver;
import eu.cloudnetservice.driver.util.ModuleUtil;
import eu.cloudnetservice.modules.signs.SharedChannelMessageListener;
import eu.cloudnetservice.modules.signs.platform.SignsPlatformListener;
import eu.cloudnetservice.modules.signs.platform.minestom.functionality.SignInteractListener;
import eu.cloudnetservice.modules.signs.platform.minestom.functionality.SignsCommand;
import net.minestom.server.MinecraftServer;
import net.minestom.server.extensions.Extension;

public class MinestomSignsExtension extends Extension {

  @Override
  public void initialize() {
    var management = new MinestomSignManagement();
    management.registerToServiceRegistry();
    management.initialize();

    // cloudnet listener
    CloudNetDriver.instance().eventManager().registerListeners(
      new SharedChannelMessageListener(management),
      new SignsPlatformListener(management));

    MinecraftServer.getCommandManager().register(new SignsCommand(management));
    new SignInteractListener(management);
  }

  @Override
  public void terminate() {
    ModuleUtil.unregisterAll(this.getClass().getClassLoader());
  }
}
