/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.ext.cloudperms.sponge;

import com.google.inject.Inject;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.cloudperms.PermissionsUpdateListener;
import de.dytanic.cloudnet.ext.cloudperms.sponge.service.CloudPermsPermissionService;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.event.lifecycle.ProvideServiceEvent;
import org.spongepowered.api.event.lifecycle.StartingEngineEvent;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

@Plugin("cloudnet_cloudperms")
public final class SpongeCloudPermissionsPlugin {

  private final PluginContainer plugin;
  private final PermissionService service;

  @Inject
  public SpongeCloudPermissionsPlugin(@NotNull PluginContainer pluginContainer) {
    this.plugin = pluginContainer;
    this.service = new CloudPermsPermissionService(CloudNetDriver.getInstance().getPermissionManagement());
  }

  @Listener
  public void handle(@NotNull ConstructPluginEvent event) {
    Sponge.eventManager().registerListeners(
      this.plugin,
      new SpongeCloudPermissionsListener(CloudNetDriver.getInstance().getPermissionManagement()));
  }

  @Listener
  public void handle(@NotNull StartingEngineEvent<Server> event) {
    CloudNetDriver.getInstance().getEventManager().registerListener(new PermissionsUpdateListener<>(
      event.engine().scheduler().executor(this.plugin),
      player -> Sponge.server().commandManager().updateCommandTreeForPlayer(player),
      ServerPlayer::uniqueId,
      uuid -> Sponge.server().player(uuid).orElse(null),
      Sponge.server()::onlinePlayers));
  }

  @Listener
  public void handlePermissionServiceProvide(@NotNull ProvideServiceEvent.EngineScoped<PermissionService> event) {
    event.suggest(() -> this.service);
  }
}
