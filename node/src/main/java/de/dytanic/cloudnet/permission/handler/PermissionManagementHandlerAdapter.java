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

package de.dytanic.cloudnet.permission.handler;

import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public class PermissionManagementHandlerAdapter implements IPermissionManagementHandler {

  public static final IPermissionManagementHandler NO_OP = new PermissionManagementHandlerAdapter();

  @Override
  public void handleAddUser(
    @NotNull IPermissionManagement management,
    @NotNull PermissionUser user
  ) {
  }

  @Override
  public void handleUpdateUser(
    @NotNull IPermissionManagement management,
    @NotNull PermissionUser user
  ) {
  }

  @Override
  public void handleDeleteUser(
    @NotNull IPermissionManagement management,
    @NotNull PermissionUser user
  ) {
  }

  @Override
  public void handleAddGroup(
    @NotNull IPermissionManagement management,
    @NotNull PermissionGroup group
  ) {
  }

  @Override
  public void handleUpdateGroup(
    @NotNull IPermissionManagement management,
    @NotNull PermissionGroup group
  ) {
  }

  @Override
  public void handleDeleteGroup(
    @NotNull IPermissionManagement management,
    @NotNull PermissionGroup group
  ) {
  }

  @Override
  public void handleSetGroups(
    @NotNull IPermissionManagement management,
    @NotNull Collection<? extends PermissionGroup> groups
  ) {
  }

  @Override
  public void handleReloaded(@NotNull IPermissionManagement management) {
  }
}
