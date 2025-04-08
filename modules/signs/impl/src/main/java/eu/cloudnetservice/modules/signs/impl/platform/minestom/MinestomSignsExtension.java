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

package eu.cloudnetservice.modules.signs.impl.platform.minestom;

import eu.cloudnetservice.driver.impl.module.ModuleHelper;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.ext.platforminject.api.PlatformEntrypoint;
import eu.cloudnetservice.ext.platforminject.api.stereotype.Dependency;
import eu.cloudnetservice.ext.platforminject.api.stereotype.PlatformPlugin;
import eu.cloudnetservice.modules.signs.impl.platform.minestom.functionality.SignInteractListener;
import eu.cloudnetservice.modules.signs.impl.platform.minestom.functionality.SignsCommand;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import net.minestom.server.command.CommandManager;

@Singleton
@PlatformPlugin(
  platform = "minestom",
  name = "CloudNet_Signs",
  version = "@version@",
  description = "Minestom extension for the CloudNet runtime which adds sign connector support",
  authors = "CloudNetService",
  dependencies = @Dependency(name = "CloudNet-Bridge"))
public class MinestomSignsExtension implements PlatformEntrypoint {

  private final ModuleHelper moduleHelper;
  private final SignsCommand signsCommand;
  private final CommandManager commandManager;
  private final ServiceRegistry serviceRegistry;
  private final MinestomSignManagement signManagement;

  @Inject
  public MinestomSignsExtension(
    @NonNull ModuleHelper moduleHelper,
    @NonNull SignsCommand signsCommand,
    @NonNull CommandManager commandManager,
    @NonNull ServiceRegistry serviceRegistry,
    @NonNull MinestomSignManagement signManagement
  ) {
    this.moduleHelper = moduleHelper;
    this.signsCommand = signsCommand;
    this.commandManager = commandManager;
    this.serviceRegistry = serviceRegistry;
    this.signManagement = signManagement;
  }

  @Override
  public void onLoad() {
    this.signManagement.initialize();
    this.signManagement.registerToServiceRegistry(this.serviceRegistry);

    this.commandManager.register(this.signsCommand);
  }

  @Inject
  private void registerSignInteract(@NonNull SignInteractListener interactListener) {
    // just need to create a new instance
  }

  @Override
  public void onDisable() {
    this.moduleHelper.unregisterAll(this.getClass().getClassLoader());
  }
}
