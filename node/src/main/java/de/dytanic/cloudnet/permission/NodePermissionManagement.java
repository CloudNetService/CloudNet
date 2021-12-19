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

package de.dytanic.cloudnet.permission;

import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import de.dytanic.cloudnet.permission.handler.IPermissionManagementHandler;
import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public interface NodePermissionManagement extends IPermissionManagement {

  void addGroupSilently(@NonNull PermissionGroup permissionGroup);

  void updateGroupSilently(@NonNull PermissionGroup permissionGroup);

  void deleteGroupSilently(@NonNull PermissionGroup permissionGroup);

  void setGroupsSilently(@Nullable Collection<? extends PermissionGroup> groups);

  @NonNull IPermissionManagementHandler permissionManagementHandler();

  void permissionManagementHandler(@NonNull IPermissionManagementHandler permissionManagementHandler);
}
