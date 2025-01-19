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

package eu.cloudnetservice.modules.signs.impl.platform.bukkit;

import eu.cloudnetservice.driver.impl.module.ModuleHelper;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.ext.platforminject.api.PlatformEntrypoint;
import eu.cloudnetservice.ext.platforminject.api.stereotype.Command;
import eu.cloudnetservice.ext.platforminject.api.stereotype.Dependency;
import eu.cloudnetservice.ext.platforminject.api.stereotype.PlatformPlugin;
import eu.cloudnetservice.modules.signs.impl.platform.bukkit.functionality.SignInteractListener;
import eu.cloudnetservice.modules.signs.impl.platform.bukkit.functionality.SignsCommand;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

@Singleton
@PlatformPlugin(
  api = "1.13",
  platform = "bukkit",
  name = "CloudNet-Signs",
  version = "@version@",
  description = "Bukkit extension for the CloudNet runtime which adds sign connector support",
  authors = "CloudNetService",
  dependencies = @Dependency(name = "CloudNet-Bridge"),
  commands = @Command(
    name = "cloudsign",
    permission = "cloudnet.command.cloudsign",
    aliases = {"cs", "signs", "cloudsigns"}))
public class BukkitSignsPlugin implements PlatformEntrypoint {

  private final JavaPlugin plugin;
  private final SignsCommand signsCommand;
  private final ModuleHelper moduleHelper;
  private final PluginManager pluginManager;
  private final ServiceRegistry serviceRegistry;
  private final BukkitSignManagement signManagement;
  private final SignInteractListener signInteractListener;

  @Inject
  public BukkitSignsPlugin(
    @NonNull JavaPlugin plugin,
    @NonNull SignsCommand signsCommand,
    @NonNull ModuleHelper moduleHelper,
    @NonNull PluginManager pluginManager,
    @NonNull ServiceRegistry serviceRegistry,
    @NonNull BukkitSignManagement signManagement,
    @NonNull SignInteractListener signInteractListener
  ) {
    this.plugin = plugin;
    this.signsCommand = signsCommand;
    this.moduleHelper = moduleHelper;
    this.pluginManager = pluginManager;
    this.serviceRegistry = serviceRegistry;
    this.signManagement = signManagement;
    this.signInteractListener = signInteractListener;
  }

  @Override
  public void onLoad() {
    this.signManagement.initialize();
    this.signManagement.registerToServiceRegistry(this.serviceRegistry);
    // bukkit command
    var pluginCommand = this.plugin.getCommand("cloudsign");
    if (pluginCommand != null) {
      pluginCommand.setExecutor(this.signsCommand);
      pluginCommand.setTabCompleter(this.signsCommand);
    }

    // bukkit listeners
    this.pluginManager.registerEvents(this.signInteractListener, this.plugin);
  }

  @Override
  public void onDisable() {
    this.moduleHelper.unregisterAll(this.getClass().getClassLoader());
  }
}
