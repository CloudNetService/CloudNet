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

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.NullCompletableTask;
import de.dytanic.cloudnet.driver.permission.DefaultCachedPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import java.util.Collection;
import java.util.Collections;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class ClusterSynchronizedPermissionManagement extends DefaultCachedPermissionManagement implements
  NodePermissionManagement {

  @Override
  public @NotNull ITask<IPermissionUser> addUserAsync(@NotNull IPermissionUser permissionUser) {
    Preconditions.checkNotNull(permissionUser);
    if (this.getPermissionManagementHandler() != null) {
      this.getPermissionManagementHandler().handleAddUser(this, permissionUser);
    }
    this.permissionUserCache.put(permissionUser.getUniqueId(), permissionUser);
    return this.addUserWithoutClusterSyncAsync(permissionUser);
  }

  @Override
  public @NotNull ITask<Void> updateUserAsync(@NotNull IPermissionUser permissionUser) {
    Preconditions.checkNotNull(permissionUser);
    if (this.getPermissionManagementHandler() != null) {
      this.getPermissionManagementHandler().handleUpdateUser(this, permissionUser);
    }
    return this.updateUserWithoutClusterSyncAsync(permissionUser);
  }

  @Override
  public @NotNull ITask<Boolean> deleteUserAsync(@NotNull String name) {
    Preconditions.checkNotNull(name);

    CompletableTask<Boolean> task = new CompletableTask<>();

    this.getUsersAsync(name).onComplete(users -> {
      boolean success = !users.isEmpty();
      for (IPermissionUser user : users) {
        success = success && this.deleteUserAsync(user).getDef(false);
      }
      task.complete(success);
    }).onCancelled(listITask -> task.cancel(true)).onFailure(throwable -> task.complete(false));

    return task;
  }

  @Override
  public @NotNull ITask<Boolean> deleteUserAsync(@NotNull IPermissionUser permissionUser) {
    Preconditions.checkNotNull(permissionUser);
    if (this.getPermissionManagementHandler() != null) {
      this.getPermissionManagementHandler().handleDeleteUser(this, permissionUser);
    }
    return this.deleteUserWithoutClusterSyncAsync(permissionUser);
  }

  @Override
  public @NotNull ITask<Void> setUsersAsync(@Nullable Collection<? extends IPermissionUser> users) {
    if (users == null) {
      users = Collections.emptyList();
    }
    if (this.getPermissionManagementHandler() != null) {
      this.getPermissionManagementHandler().handleSetUsers(this, users);
    }

    return this.setUsersWithoutClusterSyncAsync(users);
  }

  @Override
  public @NotNull ITask<IPermissionGroup> addGroupAsync(@NotNull IPermissionGroup permissionGroup) {
    Preconditions.checkNotNull(permissionGroup);

    this.testPermissible(permissionGroup);

    if (this.getPermissionManagementHandler() != null) {
      this.getPermissionManagementHandler().handleAddGroup(this, permissionGroup);
    }
    return this.addGroupWithoutClusterSyncAsync(permissionGroup);
  }

  @Override
  public @NotNull ITask<Void> updateGroupAsync(@NotNull IPermissionGroup permissionGroup) {
    Preconditions.checkNotNull(permissionGroup);

    this.testPermissible(permissionGroup);

    if (this.getPermissionManagementHandler() != null) {
      this.getPermissionManagementHandler().handleUpdateGroup(this, permissionGroup);
    }
    return this.updateGroupWithoutClusterSyncAsync(permissionGroup);
  }

  @Override
  public @NotNull ITask<Void> deleteGroupAsync(@NotNull IPermissionGroup permissionGroup) {
    if (this.getPermissionManagementHandler() != null) {
      this.getPermissionManagementHandler().handleDeleteGroup(this, permissionGroup);
    }

    return this.deleteGroupWithoutClusterSyncAsync(permissionGroup);
  }

  @Override
  public @NotNull ITask<Void> deleteGroupAsync(@NotNull String name) {
    CompletableTask<Void> task = new NullCompletableTask<>();
    this.getGroupAsync(name).onComplete(permissionGroup -> {
      if (permissionGroup == null) {
        task.call();
        return;
      }
      this.deleteGroupAsync(permissionGroup).onComplete($ -> task.call());
    });
    return task;
  }

  @Override
  public @NotNull ITask<Void> setGroupsAsync(Collection<? extends IPermissionGroup> groups) {
    if (groups == null) {
      groups = Collections.emptyList();
    }
    if (this.getPermissionManagementHandler() != null) {
      this.getPermissionManagementHandler().handleSetGroups(this, groups);
    }
    return this.setGroupsWithoutClusterSyncAsync(groups);
  }

  public void setGroups(Collection<? extends IPermissionGroup> groups) {
    if (groups == null) {
      groups = Collections.emptyList();
    }
    if (this.getPermissionManagementHandler() != null) {
      this.getPermissionManagementHandler().handleSetGroups(this, groups);
    }
    this.setGroupsWithoutClusterSyncAsync(groups);
  }

  public abstract ITask<IPermissionUser> addUserWithoutClusterSyncAsync(IPermissionUser permissionUser);

  public abstract ITask<Void> updateUserWithoutClusterSyncAsync(IPermissionUser permissionUser);

  public abstract ITask<Boolean> deleteUserWithoutClusterSyncAsync(IPermissionUser permissionUser);

  public abstract ITask<Void> setUsersWithoutClusterSyncAsync(Collection<? extends IPermissionUser> users);

  public abstract ITask<IPermissionGroup> addGroupWithoutClusterSyncAsync(IPermissionGroup permissionGroup);

  public abstract ITask<Void> updateGroupWithoutClusterSyncAsync(IPermissionGroup permissionGroup);

  public abstract ITask<Void> deleteGroupWithoutClusterSyncAsync(String group);

  public abstract ITask<Void> deleteGroupWithoutClusterSyncAsync(IPermissionGroup group);

  public abstract ITask<Void> setGroupsWithoutClusterSyncAsync(Collection<? extends IPermissionGroup> groups);

  public abstract boolean needsDatabaseSync();
}
