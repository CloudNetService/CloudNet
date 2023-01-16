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

package eu.cloudnetservice.modules.cloudperms.velocity;

import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.api.permission.PermissionSubject;
import com.velocitypowered.api.proxy.Player;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.ext.platforminject.api.stereotype.ProvidesFor;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@Singleton
@ProvidesFor(platform = "velocity", types = PermissionProvider.class)
final class VelocityCloudPermissionProvider implements PermissionProvider {

  private final PermissionManagement permissionsManagement;

  @Inject
  public VelocityCloudPermissionProvider(@NonNull PermissionManagement permissionsManagement) {
    this.permissionsManagement = permissionsManagement;
  }

  @Override
  public @Nullable PermissionFunction createFunction(@Nullable PermissionSubject subject) {
    return subject instanceof Player player
      ? new VelocityCloudPermissionFunction(player.getUniqueId(), this.permissionsManagement)
      : null;
  }
}
