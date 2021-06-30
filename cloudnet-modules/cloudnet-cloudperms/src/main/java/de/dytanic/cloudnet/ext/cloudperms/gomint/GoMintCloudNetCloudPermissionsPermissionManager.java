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

package de.dytanic.cloudnet.ext.cloudperms.gomint;

import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.driver.permission.Permission;
import io.gomint.entity.EntityPlayer;
import io.gomint.permission.Group;
import io.gomint.permission.PermissionManager;

public final class GoMintCloudNetCloudPermissionsPermissionManager implements PermissionManager {

  private final EntityPlayer player;

  private final IPermissionManagement permissionManagement;

  public GoMintCloudNetCloudPermissionsPermissionManager(EntityPlayer player,
    IPermissionManagement permissionManagement) {
    this.player = player;
    this.permissionManagement = permissionManagement;
  }

  @Override
  public boolean has(String permission) {
    if (permission == null) {
      return false;
    }

    IPermissionUser permissionUser = this.getUser();
    return permissionUser != null && this.permissionManagement.hasPermission(permissionUser, permission);
  }

  @Override
  public boolean has(String permission, boolean defaultValue) {
    return this.has(permission) || defaultValue;
  }

  @Override
  public PermissionManager addGroup(Group group) {
    return this;
  }

  @Override
  public PermissionManager removeGroup(Group group) {
    return this;
  }

  @Override
  public PermissionManager permission(String permission, boolean value) {
    IPermissionUser permissionUser = this.getUser();
    permissionUser.addPermission(new Permission(permission, value ? 1 : -1));
    this.permissionManagement.updateUser(permissionUser);

    return this;
  }

  @Override
  public PermissionManager remove(String permission) {
    IPermissionUser permissionUser = this.getUser();
    permissionUser.removePermission(permission);
    this.permissionManagement.updateUser(permissionUser);

    return this;
  }

  @Override
  public PermissionManager toggleOp() {
    return this;
  }

  private IPermissionUser getUser() {
    return this.permissionManagement.getUser(this.player.uuid());
  }

  public EntityPlayer getPlayer() {
    return this.player;
  }

}
