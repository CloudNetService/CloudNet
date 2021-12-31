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

package de.dytanic.cloudnet.permission.command;

import de.dytanic.cloudnet.command.source.DriverCommandSource;
import de.dytanic.cloudnet.driver.permission.Permission;
import de.dytanic.cloudnet.driver.permission.PermissionManagement;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import lombok.NonNull;

public final class PermissionUserCommandSource extends DriverCommandSource {

  private final PermissionUser permissionUser;
  private final PermissionManagement permissionManagement;

  public PermissionUserCommandSource(
    @NonNull PermissionUser permissionUser,
    @NonNull PermissionManagement permissionManagement
  ) {
    this.permissionUser = permissionUser;
    this.permissionManagement = permissionManagement;
  }

  /**
   * @return the name of the backing permission user
   */
  @Override
  public @NonNull String name() {
    return this.permissionUser.name();
  }

  /**
   * @param permission the permission to check for
   * @return whether the backing permission user has the permission
   */
  @Override
  public boolean checkPermission(@NonNull String permission) {
    return this.permissionManagement.hasPermission(this.permissionUser, Permission.of(permission));
  }
}
