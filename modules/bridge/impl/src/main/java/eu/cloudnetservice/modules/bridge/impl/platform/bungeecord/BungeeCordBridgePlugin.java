/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.bridge.impl.platform.bungeecord;

import com.google.inject.Singleton;
import eu.cloudnetservice.driver.impl.module.ModuleHelper;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.ext.platforminject.api.PlatformEntrypoint;
import eu.cloudnetservice.ext.platforminject.api.stereotype.PlatformPlugin;
import eu.cloudnetservice.modules.bridge.impl.platform.bungeecord.command.BungeeCordCloudCommand;
import eu.cloudnetservice.modules.bridge.impl.platform.bungeecord.command.BungeeCordFakeReloadCommand;
import eu.cloudnetservice.modules.bridge.impl.platform.bungeecord.command.BungeeCordHubCommand;
import jakarta.inject.Inject;
import java.util.Arrays;
import lombok.NonNull;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;

@Singleton
@PlatformPlugin(
  platform = "bungeecord",
  name = "CloudNet-Bridge",
  version = "@version@",
  description = "Bridges service software support between all supported versions for easy CloudNet plugin development",
  authors = "CloudNetService")
public final class BungeeCordBridgePlugin implements PlatformEntrypoint {

  private final Plugin plugin;
  private final ProxyServer proxyServer;
  private final ModuleHelper moduleHelper;
  private final PluginManager pluginManager;
  private final ServiceRegistry serviceRegistry;
  private final BungeeCordCloudCommand cloudCommand;
  private final BungeeCordBridgeManagement bridgeManagement;
  private final BungeeCordPlayerManagementListener playerListener;

  @Inject
  public BungeeCordBridgePlugin(
    @NonNull Plugin plugin,
    @NonNull ProxyServer proxyServer,
    @NonNull ModuleHelper moduleHelper,
    @NonNull PluginManager pluginManager,
    @NonNull ServiceRegistry serviceRegistry,
    @NonNull BungeeCordCloudCommand cloudCommand,
    @NonNull BungeeCordBridgeManagement bridgeManagement,
    @NonNull BungeeCordPlayerManagementListener playerListener
  ) {
    this.plugin = plugin;
    this.proxyServer = proxyServer;
    this.moduleHelper = moduleHelper;
    this.pluginManager = pluginManager;
    this.serviceRegistry = serviceRegistry;
    this.cloudCommand = cloudCommand;
    this.bridgeManagement = bridgeManagement;
    this.playerListener = playerListener;
  }

  @Override
  public void onLoad() {
    // init the management
    this.bridgeManagement.registerServices(this.serviceRegistry);
    this.bridgeManagement.postInit();
    // register the listeners
    this.pluginManager.registerListener(this.plugin, this.playerListener);
    // register the cloud command
    this.pluginManager.registerCommand(this.plugin, new BungeeCordFakeReloadCommand());
    this.pluginManager.registerCommand(this.plugin, this.cloudCommand);
    // register the hub command if requested
    if (!this.bridgeManagement.configuration().hubCommandNames().isEmpty()) {
      // convert to an array for easier access
      var names = this.bridgeManagement.configuration().hubCommandNames().toArray(new String[0]);
      // register the command
      this.pluginManager.registerCommand(this.plugin, new BungeeCordHubCommand(
        this.proxyServer,
        this.bridgeManagement,
        names[0],
        names.length > 1 ? Arrays.copyOfRange(names, 1, names.length) : new String[0]));
    }
  }

  @Override
  public void onDisable() {
    this.moduleHelper.unregisterAll(this.getClass().getClassLoader());
  }
}
