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

package eu.cloudnetservice.modules.bridge.platform.bukkit;

import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.util.ModuleHelper;
import eu.cloudnetservice.ext.platforminject.api.PlatformEntrypoint;
import eu.cloudnetservice.ext.platforminject.api.stereotype.PlatformPlugin;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

@Singleton
@PlatformPlugin(
  platform = "bukkit",
  name = "CloudNet-Bridge",
  version = "@version@",
  description = "Bridges service software support between all supported versions for easy CloudNet plugin development",
  authors = "CloudNetService"
)
public final class BukkitBridgePlugin implements PlatformEntrypoint {

  private final Plugin plugin;
  private final ModuleHelper moduleHelper;
  private final PluginManager pluginManager;
  private final ServiceRegistry serviceRegistry;
  private final BukkitBridgeManagement bridgeManagement;
  private final BukkitPlayerManagementListener playerListener;

  @Inject
  public BukkitBridgePlugin(
    @NonNull Plugin plugin,
    @NonNull ModuleHelper moduleHelper,
    @NonNull PluginManager pluginManager,
    @NonNull ServiceRegistry serviceRegistry,
    @NonNull BukkitBridgeManagement bridgeManagement,
    @NonNull BukkitPlayerManagementListener playerListener
  ) {
    this.plugin = plugin;
    this.moduleHelper = moduleHelper;
    this.pluginManager = pluginManager;
    this.serviceRegistry = serviceRegistry;
    this.bridgeManagement = bridgeManagement;
    this.playerListener = playerListener;
  }

  @Override
  public void onLoad() {
    // init the bridge management
    this.bridgeManagement.registerServices(this.serviceRegistry);
    this.bridgeManagement.postInit();
    // register the bukkit listener
    this.pluginManager.registerEvents(this.playerListener, this.plugin);
  }

  @Override
  public void onDisable() {
    this.moduleHelper.unregisterAll(this.getClass().getClassLoader());
  }
}
