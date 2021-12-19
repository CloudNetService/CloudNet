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
import lombok.NonNull;

public interface IPermissionManagementHandler {

  void handleAddUser(@NonNull IPermissionManagement management, @NonNull PermissionUser user);

  void handleUpdateUser(@NonNull IPermissionManagement management, @NonNull PermissionUser user);

  void handleDeleteUser(@NonNull IPermissionManagement management, @NonNull PermissionUser user);

  void handleAddGroup(@NonNull IPermissionManagement management, @NonNull PermissionGroup group);

  void handleUpdateGroup(@NonNull IPermissionManagement management, @NonNull PermissionGroup group);

  void handleDeleteGroup(@NonNull IPermissionManagement management, @NonNull PermissionGroup group);

  void handleSetGroups(
    @NonNull IPermissionManagement management,
    @NonNull Collection<? extends PermissionGroup> groups);

  void handleReloaded(@NonNull IPermissionManagement management);
}
