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

package eu.cloudnetservice.node.permission.command;

import eu.cloudnetservice.driver.permission.Permission;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.driver.permission.PermissionUser;
import eu.cloudnetservice.node.command.source.DriverCommandSource;
import lombok.NonNull;

/**
 * {@inheritDoc}
 */
public final class PermissionUserCommandSource extends DriverCommandSource {

  private final PermissionUser permissionUser;
  private final PermissionManagement permissionManagement;

  /**
   * Constructs a new PermissionUserCommandSource to check permissions for a user when executing commands.
   *
   * @param permissionUser       the permission user to check the permissions against.
   * @param permissionManagement the permission management for used for backing permission checks.
   */
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

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String toString() {
    return this.name();
  }
}
