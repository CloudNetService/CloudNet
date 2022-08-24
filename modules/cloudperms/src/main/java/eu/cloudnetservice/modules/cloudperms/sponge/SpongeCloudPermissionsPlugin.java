/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

import com.google.inject.Inject;
import eu.cloudnetservice.driver.CloudNetDriver;
import eu.cloudnetservice.driver.util.ModuleUtil;
import eu.cloudnetservice.modules.cloudperms.PermissionsUpdateListener;
import eu.cloudnetservice.modules.cloudperms.sponge.service.CloudPermsPermissionService;
import lombok.NonNull;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.event.lifecycle.ProvideServiceEvent;
import org.spongepowered.api.event.lifecycle.StartingEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

@Plugin("cloudnet_cloudperms")
public final class SpongeCloudPermissionsPlugin {

  private final PluginContainer plugin;
  private final PermissionService service;

  @Inject
  public SpongeCloudPermissionsPlugin(@NonNull PluginContainer pluginContainer) {
    this.plugin = pluginContainer;
    this.service = new CloudPermsPermissionService(CloudNetDriver.instance().permissionManagement());
  }

  @Listener
  public void handle(@NonNull ConstructPluginEvent event) {
    Sponge.eventManager().registerListeners(
      this.plugin,
      new SpongeCloudPermissionsListener(CloudNetDriver.instance().permissionManagement()));
  }

  @Listener
  public void handle(@NonNull StartingEngineEvent<Server> event) {
    CloudNetDriver.instance().eventManager().registerListener(new PermissionsUpdateListener<>(
      event.engine().scheduler().executor(this.plugin),
      player -> Sponge.server().commandManager().updateCommandTreeForPlayer(player),
      ServerPlayer::uniqueId,
      uuid -> Sponge.server().player(uuid).orElse(null),
      Sponge.server()::onlinePlayers));
  }

  @Listener
  public void handle(@NonNull StoppingEngineEvent<Server> event) {
    ModuleUtil.unregisterAll(this.getClass().getClassLoader());
  }

  @Listener
  public void handlePermissionServiceProvide(@NonNull ProvideServiceEvent.EngineScoped<PermissionService> event) {
    event.suggest(() -> this.service);
  }
}
