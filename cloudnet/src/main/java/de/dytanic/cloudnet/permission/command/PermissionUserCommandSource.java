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

package de.dytanic.cloudnet.permission.command;

import de.dytanic.cloudnet.command.source.DriverCommandSource;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import org.jetbrains.annotations.NotNull;

public final class PermissionUserCommandSource extends DriverCommandSource {

  private final PermissionUser permissionUser;
  private final IPermissionManagement permissionManagement;

  public PermissionUserCommandSource(
    @NotNull PermissionUser permissionUser,
    @NotNull IPermissionManagement permissionManagement
  ) {
    this.permissionUser = permissionUser;
    this.permissionManagement = permissionManagement;
  }

  @Override
  public @NotNull String getName() {
    return this.permissionUser.getName();
  }

  @Override
  public boolean checkPermission(@NotNull String permission) {
    return this.permissionManagement.hasPermission(this.permissionUser, permission);
  }
}
