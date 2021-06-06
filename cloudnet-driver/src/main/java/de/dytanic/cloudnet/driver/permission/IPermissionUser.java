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

package de.dytanic.cloudnet.driver.permission;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IPermissionUser extends IPermissible {

  @NotNull
  UUID getUniqueId();

  Collection<PermissionUserGroupInfo> getGroups();

  @Nullable
  String getHashedPassword();

  void changePassword(String password);

  boolean checkPassword(String password);


  default IPermissionUser addGroup(@NotNull String group) {
    return this.addGroup(group, 0L);
  }

  default IPermissionUser addGroup(@NotNull String group, long time, TimeUnit timeUnit) {
    return this.addGroup(group, (System.currentTimeMillis() + timeUnit.toMillis(time)));
  }

  default IPermissionUser addGroup(@NotNull String group, long timeOutMillis) {
    PermissionUserGroupInfo groupInfo = this.getGroups().stream()
      .filter(permissionUserGroupInfo -> permissionUserGroupInfo.getGroup().equalsIgnoreCase(group))
      .findFirst().orElse(null);

    if (groupInfo != null) {
      this.removeGroup(groupInfo.getGroup());
    }

    groupInfo = new PermissionUserGroupInfo(group, timeOutMillis);

    this.getGroups().add(groupInfo);
    return this;
  }

  default IPermissionUser removeGroup(@NotNull String group) {
    Collection<PermissionUserGroupInfo> groupInfo = this.getGroups().stream()
      .filter(permissionUserGroupInfo -> permissionUserGroupInfo.getGroup().equalsIgnoreCase(group))
      .collect(Collectors.toList());

    this.getGroups().removeAll(groupInfo);

    return this;
  }

  default boolean inGroup(@NotNull String group) {
    return this.getGroups().stream()
      .anyMatch(permissionUserGroupInfo -> permissionUserGroupInfo.getGroup().equalsIgnoreCase(group));
  }
}
