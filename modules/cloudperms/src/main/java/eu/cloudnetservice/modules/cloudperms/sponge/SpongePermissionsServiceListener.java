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

import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.ext.platforminject.api.mapping.Container;
import eu.cloudnetservice.modules.cloudperms.sponge.service.CloudPermsPermissionService;
import lombok.NonNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ProvideServiceEvent;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.plugin.PluginContainer;

final class SpongePermissionsServiceListener {

  public SpongePermissionsServiceListener(@NonNull Container<PluginContainer> platformData) {
    Sponge.eventManager().registerListeners(platformData.container(), this);
  }

  @Listener
  public void handlePermissionServiceProvide(@NonNull ProvideServiceEvent.EngineScoped<PermissionService> event) {
    event.suggest(() -> {
      // get the in use permission service of the current environment
      var permissionManagement = InjectionLayer.ext().instance(PermissionManagement.class);
      return new CloudPermsPermissionService(permissionManagement);
    });
  }
}
