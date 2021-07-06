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

package de.dytanic.cloudnet.ext.cloudperms.velocity;

import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.api.permission.PermissionSubject;
import com.velocitypowered.api.proxy.Player;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import org.checkerframework.checker.optional.qual.MaybePresent;

public class VelocityCloudNetCloudPermissionsPermissionProvider implements PermissionProvider {

  private final IPermissionManagement permissionsManagement;

  public VelocityCloudNetCloudPermissionsPermissionProvider(IPermissionManagement permissionsManagement) {
    this.permissionsManagement = permissionsManagement;
  }

  @Override
  public @MaybePresent PermissionFunction createFunction(@MaybePresent PermissionSubject subject) {
    return subject instanceof Player
      ? new VelocityCloudNetCloudPermissionsPermissionFunction(((Player) subject).getUniqueId(),
      this.permissionsManagement)
      : null;
  }
}
