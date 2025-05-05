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

package eu.cloudnetservice.modules.signs.impl.platform.nukkit;

import cn.nukkit.command.PluginCommand;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.plugin.PluginManager;
import eu.cloudnetservice.driver.impl.module.ModuleHelper;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.ext.platforminject.api.PlatformEntrypoint;
import eu.cloudnetservice.ext.platforminject.api.stereotype.Command;
import eu.cloudnetservice.ext.platforminject.api.stereotype.Dependency;
import eu.cloudnetservice.ext.platforminject.api.stereotype.PlatformPlugin;
import eu.cloudnetservice.modules.signs.impl.platform.nukkit.functionality.SignInteractListener;
import eu.cloudnetservice.modules.signs.impl.platform.nukkit.functionality.SignsCommand;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
@PlatformPlugin(
  platform = "nukkit",
  name = "CloudNet-Signs",
  version = "@version@",
  description = "Nukkit extension for the CloudNet runtime which adds sign connector support",
  authors = "CloudNetService",
  dependencies = @Dependency(name = "CloudNet-Bridge"),
  commands = @Command(
    name = "cloudsign",
    permission = "cloudnet.command.cloudsign",
    aliases = {"cs", "signs", "cloudsigns"}))
public class NukkitSignsPlugin implements PlatformEntrypoint {

  private final PluginBase plugin;
  private final ModuleHelper moduleHelper;
  private final SignsCommand signsCommand;
  private final PluginManager pluginManager;
  private final ServiceRegistry serviceRegistry;
  private final NukkitSignManagement signManagement;
  private final SignInteractListener interactListener;

  @Inject
  public NukkitSignsPlugin(
    @NonNull PluginBase plugin,
    @NonNull ModuleHelper moduleHelper,
    @NonNull SignsCommand signsCommand,
    @NonNull PluginManager pluginManager,
    @NonNull ServiceRegistry serviceRegistry,
    @NonNull NukkitSignManagement signManagement,
    @NonNull SignInteractListener interactListener
  ) {
    this.plugin = plugin;
    this.moduleHelper = moduleHelper;
    this.signsCommand = signsCommand;
    this.pluginManager = pluginManager;
    this.serviceRegistry = serviceRegistry;
    this.signManagement = signManagement;
    this.interactListener = interactListener;
  }

  @Override
  public void onLoad() {
    this.signManagement.initialize();
    this.signManagement.registerToServiceRegistry(this.serviceRegistry);
    // command
    var pluginCommand = (PluginCommand<?>) this.plugin.getCommand("cloudsign");
    if (pluginCommand != null) {
      pluginCommand.setExecutor(this.signsCommand);
    }
    // nukkit listeners
    this.pluginManager.registerEvents(this.interactListener, this.plugin);
  }

  @Override
  public void onDisable() {
    this.moduleHelper.unregisterAll(this.getClass().getClassLoader());
  }
}
