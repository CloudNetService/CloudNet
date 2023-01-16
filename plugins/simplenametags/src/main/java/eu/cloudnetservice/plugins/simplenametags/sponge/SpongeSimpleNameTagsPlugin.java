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

package eu.cloudnetservice.plugins.simplenametags.sponge;

import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.ext.platforminject.api.PlatformEntrypoint;
import eu.cloudnetservice.ext.platforminject.api.stereotype.Dependency;
import eu.cloudnetservice.ext.platforminject.api.stereotype.PlatformPlugin;
import eu.cloudnetservice.plugins.simplenametags.SimpleNameTagsManager;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import lombok.NonNull;
import org.spongepowered.api.Server;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.plugin.PluginContainer;

@Singleton
@PlatformPlugin(
  platform = "sponge",
  name = "CloudNet-SimpleNameTags",
  version = "{project.build.version}",
  authors = "CloudNetService",
  homepage = "https://cloudnetservice.eu",
  description = "Adds prefix, suffix and display name support to all server platforms",
  dependencies = {
    @Dependency(name = "spongeapi", version = "8.0.0"),
    @Dependency(name = "CloudNet-CloudPerms", version = "{project.build.version}")
  }
)
public final class SpongeSimpleNameTagsPlugin implements PlatformEntrypoint {

  private final PluginContainer pluginContainer;
  private final SimpleNameTagsManager<ServerPlayer> nameTagsManager;
  private final org.spongepowered.api.event.EventManager spongeEventManager;

  @Inject
  public SpongeSimpleNameTagsPlugin(
    @NonNull Server server,
    @NonNull @Named("sync") Scheduler scheduler,
    @NonNull PluginContainer pluginContainer,
    @NonNull EventManager eventManager,
    @NonNull PermissionManagement permissionManagement,
    @NonNull org.spongepowered.api.event.EventManager spongeEventManager
  ) {
    this.pluginContainer = pluginContainer;
    this.spongeEventManager = spongeEventManager;
    this.nameTagsManager = new SpongeSimpleNameTagsManager(
      server,
      scheduler.executor(pluginContainer),
      eventManager,
      permissionManagement);
  }

  @Override
  public void onLoad() {
    this.spongeEventManager.registerListeners(this.pluginContainer, this);
  }

  @Listener
  public void handle(@NonNull ServerSideConnectionEvent.Join event, @First @NonNull ServerPlayer player) {
    this.nameTagsManager.updateNameTagsFor(player);
  }
}
