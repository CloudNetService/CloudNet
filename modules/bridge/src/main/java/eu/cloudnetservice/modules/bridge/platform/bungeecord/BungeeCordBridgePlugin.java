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

package eu.cloudnetservice.modules.bridge.platform.bungeecord;

import eu.cloudnetservice.driver.util.ModuleUtil;
import eu.cloudnetservice.modules.bridge.platform.PlatformBridgeManagement;
import eu.cloudnetservice.modules.bridge.platform.bungeecord.command.BungeeCordCloudCommand;
import eu.cloudnetservice.modules.bridge.platform.bungeecord.command.BungeeCordFakeReloadCommand;
import eu.cloudnetservice.modules.bridge.platform.bungeecord.command.BungeeCordHubCommand;
import eu.cloudnetservice.modules.bridge.player.NetworkPlayerProxyInfo;
import eu.cloudnetservice.wrapper.Wrapper;
import java.util.Arrays;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

public final class BungeeCordBridgePlugin extends Plugin {

  @Override
  public void onEnable() {
    // init the management
    PlatformBridgeManagement<ProxiedPlayer, NetworkPlayerProxyInfo> management = new BungeeCordBridgeManagement();
    management.registerServices(Wrapper.instance().serviceRegistry());
    management.postInit();
    // register the listeners
    ProxyServer.getInstance().getPluginManager().registerListener(
      this,
      new BungeeCordPlayerManagementListener(this, management));
    // register the cloud command
    ProxyServer.getInstance().getPluginManager().registerCommand(this, new BungeeCordFakeReloadCommand());
    ProxyServer.getInstance().getPluginManager().registerCommand(this, new BungeeCordCloudCommand(management));
    // register the hub command if requested
    if (!management.configuration().hubCommandNames().isEmpty()) {
      // convert to an array for easier access
      var names = management.configuration().hubCommandNames().toArray(new String[0]);
      // register the command
      ProxyServer.getInstance().getPluginManager().registerCommand(this, new BungeeCordHubCommand(
        management,
        names[0],
        names.length > 1 ? Arrays.copyOfRange(names, 1, names.length) : new String[0]));
    }
  }

  @Override
  public void onDisable() {
    ModuleUtil.unregisterAll(this.getClass().getClassLoader());
  }
}
