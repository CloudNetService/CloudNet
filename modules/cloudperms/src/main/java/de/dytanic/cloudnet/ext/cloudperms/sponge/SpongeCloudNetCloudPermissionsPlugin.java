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
import de.dytanic.cloudnet.ext.cloudperms.sponge.service.CloudPermsPermissionService;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.event.lifecycle.ProvideServiceEvent;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

@Plugin("cloudnet_cloudperms")
public final class SpongeCloudNetCloudPermissionsPlugin {

  private final PluginContainer plugin;
  private final PermissionService service;

  @Inject
  public SpongeCloudNetCloudPermissionsPlugin(@NotNull PluginContainer pluginContainer) {
    this.plugin = pluginContainer;
    this.service = new CloudPermsPermissionService(CloudNetDriver.getInstance().getPermissionManagement());
  }

  @Listener
  public void onEnable(@NotNull ConstructPluginEvent event) {
    Sponge.eventManager().registerListeners(
      this.plugin,
      new SpongeCloudNetPermissionsListener(CloudNetDriver.getInstance().getPermissionManagement()));
  }

  @Listener
  public void handlePermissionServiceProvide(@NotNull ProvideServiceEvent.EngineScoped<PermissionService> event) {
    event.suggest(() -> this.service);
  }
}
