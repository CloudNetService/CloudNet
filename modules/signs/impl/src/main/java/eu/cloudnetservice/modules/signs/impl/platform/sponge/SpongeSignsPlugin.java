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

package eu.cloudnetservice.modules.signs.impl.platform.sponge;

import eu.cloudnetservice.driver.impl.module.ModuleHelper;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.ext.platforminject.api.PlatformEntrypoint;
import eu.cloudnetservice.ext.platforminject.api.stereotype.ConstructionListener;
import eu.cloudnetservice.ext.platforminject.api.stereotype.Dependency;
import eu.cloudnetservice.ext.platforminject.api.stereotype.PlatformPlugin;
import eu.cloudnetservice.modules.signs.impl.platform.sponge.functionality.SignInteractListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.plugin.PluginContainer;

@Singleton
@PlatformPlugin(
  platform = "sponge",
  name = "CloudNet-Signs",
  version = "@version@",
  description = "Sponge extension for the CloudNet runtime which adds sign connector support",
  authors = "CloudNetService",
  dependencies = {
    @Dependency(name = "spongeapi", version = "[8.0.0,)"),
    @Dependency(name = "CloudNet-Bridge", version = "@version@")
  }
)
@ConstructionListener(CommandRegistrationListener.class)
public class SpongeSignsPlugin implements PlatformEntrypoint {

  private final PluginContainer plugin;
  private final ModuleHelper moduleHelper;
  private final EventManager eventManager;
  private final ServiceRegistry serviceRegistry;
  private final SpongeSignManagement signManagement;
  private final SignInteractListener interactListener;

  @Inject
  public SpongeSignsPlugin(
    @NonNull PluginContainer plugin,
    @NonNull ModuleHelper moduleHelper,
    @NonNull EventManager eventManager,
    @NonNull ServiceRegistry serviceRegistry,
    @NonNull SpongeSignManagement signManagement,
    @NonNull SignInteractListener interactListener
  ) {
    this.plugin = plugin;
    this.moduleHelper = moduleHelper;
    this.eventManager = eventManager;
    this.serviceRegistry = serviceRegistry;
    this.signManagement = signManagement;
    this.interactListener = interactListener;
  }

  @Override
  public void onLoad() {
    this.signManagement.initialize();
    this.signManagement.registerToServiceRegistry(this.serviceRegistry);

    this.eventManager.registerListeners(this.plugin, this.interactListener);
  }

  @Override
  public void onDisable() {
    this.moduleHelper.unregisterAll(this.getClass().getClassLoader());
  }
}
