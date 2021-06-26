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
import com.velocitypowered.api.permission.Tristate;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import java.util.UUID;

public final class VelocityCloudNetCloudPermissionsPermissionFunction implements PermissionFunction {

  private final UUID uniqueId;
  private final IPermissionManagement permissionsManagement;

  public VelocityCloudNetCloudPermissionsPermissionFunction(UUID uniqueId,
    IPermissionManagement permissionsManagement) {
    this.uniqueId = uniqueId;
    this.permissionsManagement = permissionsManagement;
  }

  @Override
  public Tristate getPermissionValue(String permission) {
    if (permission == null) {
      return Tristate.FALSE;
    }

    IPermissionUser permissionUser = this.permissionsManagement.getUser(this.uniqueId);
    return (permissionUser != null && this.permissionsManagement.hasPermission(permissionUser, permission)) ?
      Tristate.TRUE : Tristate.FALSE;
  }

  public UUID getUniqueId() {
    return this.uniqueId;
  }
}
