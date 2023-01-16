/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.cloudperms.bungee;

import eu.cloudnetservice.driver.util.ModuleHelper;
import eu.cloudnetservice.ext.platforminject.api.PlatformEntrypoint;
import eu.cloudnetservice.ext.platforminject.api.stereotype.PlatformPlugin;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;

@Singleton
@PlatformPlugin(
  platform = "bungeecord",
  name = "CloudNet-CloudPerms",
  authors = "CloudNetService",
  version = "{project.build.version}",
  description = "BungeeCord extension which implement the permission management system from CloudNet into BungeeCord"
)
public final class BungeeCloudPermissionsPlugin implements PlatformEntrypoint {

  private final Plugin plugin;
  private final ModuleHelper moduleHelper;
  private final PluginManager pluginManager;
  private final BungeeCloudPermissionsPlayerListener playerListener;

  @Inject
  public BungeeCloudPermissionsPlugin(
    @NonNull Plugin plugin,
    @NonNull ModuleHelper moduleHelper,
    @NonNull PluginManager pluginManager,
    @NonNull BungeeCloudPermissionsPlayerListener playerListener
  ) {
    this.plugin = plugin;
    this.moduleHelper = moduleHelper;
    this.pluginManager = pluginManager;
    this.playerListener = playerListener;
  }

  @Override
  public void onLoad() {
    this.pluginManager.registerListener(this.plugin, this.playerListener);
  }

  @Override
  public void onDisable() {
    this.moduleHelper.unregisterAll(this.getClass().getClassLoader());
  }
}
