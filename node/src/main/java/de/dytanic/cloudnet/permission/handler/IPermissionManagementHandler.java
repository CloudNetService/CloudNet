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

public interface IPermissionManagementHandler {

  void handleAddUser(@NotNull IPermissionManagement management, @NotNull PermissionUser user);

  void handleUpdateUser(@NotNull IPermissionManagement management, @NotNull PermissionUser user);

  void handleDeleteUser(@NotNull IPermissionManagement management, @NotNull PermissionUser user);

  void handleAddGroup(@NotNull IPermissionManagement management, @NotNull PermissionGroup group);

  void handleUpdateGroup(@NotNull IPermissionManagement management, @NotNull PermissionGroup group);

  void handleDeleteGroup(@NotNull IPermissionManagement management, @NotNull PermissionGroup group);

  void handleSetGroups(
    @NotNull IPermissionManagement management,
    @NotNull Collection<? extends PermissionGroup> groups);

  void handleReloaded(@NotNull IPermissionManagement management);
}
