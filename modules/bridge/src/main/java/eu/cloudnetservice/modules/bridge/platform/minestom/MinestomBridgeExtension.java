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

package eu.cloudnetservice.modules.bridge.platform.minestom;

import eu.cloudnetservice.driver.util.ModuleUtil;
import eu.cloudnetservice.wrapper.Wrapper;
import net.minestom.server.extensions.Extension;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.extras.bungee.BungeeCordProxy;
import net.minestom.server.extras.velocity.VelocityProxy;

public final class MinestomBridgeExtension extends Extension {

  @Override
  public void initialize() {
    var management = new MinestomBridgeManagement();
    management.registerServices(Wrapper.instance().serviceRegistry());
    management.postInit();

    // register the minestom listener
    new MinestomPlayerManagementListener(management);

    // force initialize the bungeecord proxy forwarding
    if (!VelocityProxy.isEnabled()) {
      BungeeCordProxy.enable();
    }

    // using bungeecord and mojang auth will not work, we can't do anything about it. Just send a warning
    if (!VelocityProxy.isEnabled() && MojangAuth.isEnabled()) {
      this.getLogger().warn(
        "Be aware that using BungeeCord player info forwarding in combination with Mojang authentication will not work!");
    }
  }

  @Override
  public void terminate() {
    ModuleUtil.unregisterAll(this.getClass().getClassLoader());
  }
}
