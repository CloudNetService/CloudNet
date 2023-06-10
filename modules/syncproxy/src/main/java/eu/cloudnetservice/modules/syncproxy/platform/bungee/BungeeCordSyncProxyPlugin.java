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

package eu.cloudnetservice.modules.syncproxy.platform.bungee;

import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.util.ModuleHelper;
import eu.cloudnetservice.ext.platforminject.api.PlatformEntrypoint;
import eu.cloudnetservice.ext.platforminject.api.stereotype.Dependency;
import eu.cloudnetservice.ext.platforminject.api.stereotype.PlatformPlugin;
import eu.cloudnetservice.modules.syncproxy.platform.listener.SyncProxyCloudListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;

@Singleton
@PlatformPlugin(
  platform = "bungeecord",
  name = "CloudNet-SyncProxy",
  version = "{project.build.version}",
  description = "CloudNet extension which serves proxy utils with CloudNet support",
  authors = "CloudNetService",
  dependencies = @Dependency(name = "CloudNet-Bridge"))
public final class BungeeCordSyncProxyPlugin implements PlatformEntrypoint {

  private final Plugin plugin;
  private final ModuleHelper moduleHelper;
  private final EventManager eventManager;
  private final ServiceRegistry serviceRegistry;
  private final BungeeCordSyncProxyManagement syncProxyManagement;

  @Inject
  public BungeeCordSyncProxyPlugin(
    @NonNull Plugin plugin,
    @NonNull ModuleHelper moduleHelper,
    @NonNull EventManager eventManager,
    @NonNull ServiceRegistry serviceRegistry,
    @NonNull BungeeCordSyncProxyManagement syncProxyManagement
  ) {
    this.plugin = plugin;
    this.moduleHelper = moduleHelper;
    this.eventManager = eventManager;
    this.serviceRegistry = serviceRegistry;
    this.syncProxyManagement = syncProxyManagement;
  }

  @Override
  public void onLoad() {
    // register the SyncProxyManagement in our service registry
    this.syncProxyManagement.registerService(this.serviceRegistry);
    // register the event listener to handle service updates
    this.eventManager.registerListener(new SyncProxyCloudListener<>(this.syncProxyManagement));
  }

  @Inject
  public void registerListener(@NonNull PluginManager pluginManager, @NonNull BungeeCordSyncProxyListener listener) {
    pluginManager.registerListener(this.plugin, listener);
  }

  @Override
  public void onDisable() {
    // unregister all listeners for cloudnet events
    this.moduleHelper.unregisterAll(this.getClass().getClassLoader());
  }
}
