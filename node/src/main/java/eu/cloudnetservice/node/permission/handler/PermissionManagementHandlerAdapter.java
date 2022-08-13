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

package eu.cloudnetservice.node.permission.handler;

import eu.cloudnetservice.driver.permission.PermissionGroup;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.driver.permission.PermissionUser;
import java.util.Collection;
import lombok.NonNull;

public class PermissionManagementHandlerAdapter implements PermissionManagementHandler {

  public static final PermissionManagementHandler NO_OP = new PermissionManagementHandlerAdapter();

  @Override
  public void handleAddUser(
    @NonNull PermissionManagement management,
    @NonNull PermissionUser user
  ) {
  }

  @Override
  public void handleUpdateUser(
    @NonNull PermissionManagement management,
    @NonNull PermissionUser user
  ) {
  }

  @Override
  public void handleDeleteUser(
    @NonNull PermissionManagement management,
    @NonNull PermissionUser user
  ) {
  }

  @Override
  public void handleAddGroup(
    @NonNull PermissionManagement management,
    @NonNull PermissionGroup group
  ) {
  }

  @Override
  public void handleUpdateGroup(
    @NonNull PermissionManagement management,
    @NonNull PermissionGroup group
  ) {
  }

  @Override
  public void handleDeleteGroup(
    @NonNull PermissionManagement management,
    @NonNull PermissionGroup group
  ) {
  }

  @Override
  public void handleSetGroups(@NonNull PermissionManagement management, @NonNull Collection<PermissionGroup> groups) {
  }

  @Override
  public void handleReloaded(@NonNull PermissionManagement management) {
  }
}
