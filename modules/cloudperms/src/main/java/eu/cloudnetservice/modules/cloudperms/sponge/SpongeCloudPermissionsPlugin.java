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

package eu.cloudnetservice.modules.cloudperms.sponge;

import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.driver.util.ModuleHelper;
import eu.cloudnetservice.ext.platforminject.api.PlatformEntrypoint;
import eu.cloudnetservice.ext.platforminject.api.stereotype.ConstructionListener;
import eu.cloudnetservice.ext.platforminject.api.stereotype.Dependency;
import eu.cloudnetservice.ext.platforminject.api.stereotype.PlatformPlugin;
import eu.cloudnetservice.modules.cloudperms.PermissionsUpdateListener;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import lombok.NonNull;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.manager.CommandManager;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.plugin.PluginContainer;

@Singleton
@PlatformPlugin(
  platform = "sponge",
  name = "CloudNet-CloudPerms",
  authors = "CloudNetService",
  version = "@version@",
  homepage = "https://cloudnetservice.eu",
  description = "Sponge extension which implement the permission management system from CloudNet into Sponge",
  dependencies = @Dependency(name = "spongeapi", version = "8.0.0")
)
@ConstructionListener(SpongePermissionsServiceListener.class)
public final class SpongeCloudPermissionsPlugin implements PlatformEntrypoint {

  private final ModuleHelper moduleHelper;
  private final PermissionManagement permissionManagement;
  private final eu.cloudnetservice.driver.event.EventManager cloudEventManager;

  private final Server server;
  private final PluginContainer plugin;
  private final Scheduler syncScheduler;
  private final EventManager eventManager;
  private final CommandManager commandManager;
  private final SpongeCloudPermissionsListener playerListener;

  @Inject
  public SpongeCloudPermissionsPlugin(
    @NonNull ModuleHelper moduleHelper,
    @NonNull PermissionManagement permissionManagement,
    @NonNull eu.cloudnetservice.driver.event.EventManager cloudEventManager,
    @NonNull Server server,
    @NonNull PluginContainer plugin,
    @NonNull @Named("sync") Scheduler syncScheduler,
    @NonNull EventManager eventManager,
    @NonNull CommandManager commandManager,
    @NonNull SpongeCloudPermissionsListener playerListener
  ) {
    this.moduleHelper = moduleHelper;
    this.permissionManagement = permissionManagement;
    this.cloudEventManager = cloudEventManager;
    this.server = server;
    this.plugin = plugin;
    this.syncScheduler = syncScheduler;
    this.eventManager = eventManager;
    this.commandManager = commandManager;
    this.playerListener = playerListener;
  }

  @Override
  public void onLoad() {
    // sponge listeners
    this.eventManager.registerListeners(this.plugin, this.playerListener);

    // internal listeners
    this.cloudEventManager.registerListener(new PermissionsUpdateListener<>(
      this.syncScheduler.executor(this.plugin),
      this.commandManager::updateCommandTreeForPlayer,
      ServerPlayer::uniqueId,
      uuid -> this.server.player(uuid).orElse(null),
      this.permissionManagement,
      Sponge.server()::onlinePlayers));
  }

  @Override
  public void onDisable() {
    this.moduleHelper.unregisterAll(this.getClass().getClassLoader());
  }
}
