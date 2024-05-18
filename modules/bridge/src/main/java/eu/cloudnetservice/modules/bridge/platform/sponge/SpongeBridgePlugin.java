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

package eu.cloudnetservice.modules.bridge.platform.sponge;

import com.google.inject.Singleton;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.util.ModuleHelper;
import eu.cloudnetservice.ext.platforminject.api.PlatformEntrypoint;
import eu.cloudnetservice.ext.platforminject.api.stereotype.Dependency;
import eu.cloudnetservice.ext.platforminject.api.stereotype.PlatformPlugin;
import jakarta.inject.Inject;
import lombok.NonNull;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.plugin.PluginContainer;

@Singleton
@PlatformPlugin(
  platform = "sponge",
  name = "CloudNet-Bridge",
  version = "@version@",
  description = "Bridges service software support between all supported versions for easy CloudNet plugin development",
  authors = "CloudNetService",
  dependencies = @Dependency(name = "spongeapi", version = "8.0.0")
)
public final class SpongeBridgePlugin implements PlatformEntrypoint {

  private final PluginContainer plugin;
  private final EventManager eventManager;
  private final ModuleHelper moduleHelper;
  private final ServiceRegistry serviceRegistry;
  private final SpongeBridgeManagement bridgeManagement;
  private final SpongePlayerManagementListener playerListener;

  @Inject
  public SpongeBridgePlugin(
    @NonNull PluginContainer plugin,
    @NonNull EventManager eventManager,
    @NonNull ModuleHelper moduleHelper,
    @NonNull ServiceRegistry serviceRegistry,
    @NonNull SpongeBridgeManagement bridgeManagement,
    @NonNull SpongePlayerManagementListener playerListener
  ) {
    this.plugin = plugin;
    this.eventManager = eventManager;
    this.moduleHelper = moduleHelper;
    this.serviceRegistry = serviceRegistry;
    this.bridgeManagement = bridgeManagement;
    this.playerListener = playerListener;
  }

  @Override
  public void onLoad() {
    this.bridgeManagement.registerServices(this.serviceRegistry);
    this.bridgeManagement.postInit();
    // register the listener
    this.eventManager.registerListeners(this.plugin, this.playerListener);
  }

  @Override
  public void onDisable() {
    this.moduleHelper.unregisterAll(this.getClass().getClassLoader());
  }
}
