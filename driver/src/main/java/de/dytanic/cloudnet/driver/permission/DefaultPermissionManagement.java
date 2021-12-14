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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class DefaultPermissionManagement implements IPermissionManagement {

  @Override
  public IPermissionManagement getChildPermissionManagement() {
    return null;
  }

  @Override
  public boolean canBeOverwritten() {
    return true;
  }

  @Override
  public PermissionGroup getHighestPermissionGroup(@NotNull PermissionUser permissionUser) {
    PermissionGroup permissionGroup = null;

    for (var group : this.getGroupsOf(permissionUser)) {
      if (permissionGroup == null) {
        permissionGroup = group;
        continue;
      }

      if (permissionGroup.getPotency() <= group.getPotency()) {
        permissionGroup = group;
      }
    }

    return permissionGroup != null ? permissionGroup : this.getDefaultPermissionGroup();
  }

  @Override
  public boolean testPermissionUser(@Nullable PermissionUser permissionUser) {
    if (permissionUser == null) {
      return false;
    }

    return this.testPermissible(permissionUser) || permissionUser.getGroups().removeIf(
      groupInfo -> groupInfo.getTimeOutMillis() > 0 && groupInfo.getTimeOutMillis() < System.currentTimeMillis());
  }

  @Override
  public boolean testPermissible(@Nullable IPermissible permissible) {
    if (permissible == null) {
      return false;
    }

    var tester = (Predicate<Permission>) permission -> permission.getTimeOutMillis() > 0
      && permission.getTimeOutMillis() < System.currentTimeMillis();

    var result = permissible.getPermissions().removeIf(tester);
    for (var entry : permissible.getGroupPermissions().entrySet()) {
      result |= entry.getValue().removeIf(tester);
    }

    return result;
  }

  @Override
  @NotNull
  public Collection<PermissionGroup> getGroupsOf(@Nullable IPermissible permissible) {
    List<PermissionGroup> permissionGroups = new ArrayList<>();

    if (permissible == null) {
      return permissionGroups;
    }

    for (var group : permissible.getGroupNames()) {
      var permissionGroup = this.getGroup(group);
      if (permissionGroup != null) {
        permissionGroups.add(permissionGroup);
      }
    }

    if (permissionGroups.isEmpty() && permissible instanceof PermissionUser) {
      permissionGroups.add(this.getDefaultPermissionGroup());
    }

    return permissionGroups;
  }

  @Override
  public @NotNull PermissionCheckResult getPermissionResult(
    @NotNull IPermissible permissible,
    @NotNull Permission permission
  ) {
    return PermissionCheckResult.fromPermission(
      this.findHighestPermission(this.collectAllPermissions(permissible, null), permission));
  }

  @Override
  public @NotNull PermissionCheckResult getGroupPermissionResult(
    @NotNull IPermissible permissible,
    @NotNull String group,
    @NotNull Permission permission
  ) {
    return this.getGroupsPermissionResult(permissible, new String[]{group}, permission);
  }

  @Override
  public @NotNull PermissionCheckResult getGroupsPermissionResult(@NotNull IPermissible permissible,
    @NotNull String[] groups,
    @NotNull Permission permission) {
    return PermissionCheckResult
      .fromPermission(this.findHighestPermission(this.collectAllPermissions(permissible, groups), permission));
  }

  @Override
  @Nullable
  public Permission findHighestPermission(@NotNull Collection<Permission> permissions, @NotNull Permission permission) {
    Permission lastMatch = null;
    // search for a better match
    for (var permissionEntry : permissions) {
      var used = lastMatch == null ? permission : lastMatch;
      // the "star" permission represents a permission which allows access to every command
      if (permissionEntry.getName().equals("*") && permissionEntry.compareTo(used) >= 0) {
        lastMatch = permissionEntry;
        continue;
      }
      // searches for "perm.*"-permissions (allowing all sub permissions of the given permission name start
      if (permissionEntry.getName().endsWith("*")
        && permission.getName().contains(permissionEntry.getName().replace("*", ""))
        && permissionEntry.compareTo(used) >= 0) {
        lastMatch = permissionEntry;
        continue;
      }
      // checks if the current permission is exactly (case-sensitive) the permission for which we are searching
      if (permission.getName().equalsIgnoreCase(permissionEntry.getName()) && permissionEntry.compareTo(used) >= 0) {
        lastMatch = permissionEntry;
      }
    }

    return lastMatch;
  }

  protected Collection<Permission> collectAllPermissions(@NotNull IPermissible permissible, @Nullable String[] groups) {
    return this.collectAllPermissionsTo(new HashSet<>(), permissible, groups);
  }

  protected Collection<Permission> collectAllPermissionsTo(@NotNull Collection<Permission> target,
    @NotNull IPermissible permissible,
    @Nullable String[] groups) {
    this.collectPermissionsInto(target, permissible, groups);
    this.collectAllGroupPermissionsInto(target, this.getGroupsOf(permissible), groups, new HashSet<>());

    return target;
  }

  protected void collectAllGroupPermissionsInto(@NotNull Collection<Permission> target,
    @NotNull Collection<PermissionGroup> groups,
    @Nullable String[] taskGroups, @NotNull Collection<String> travelledGroups) {
    for (var permissionGroup : groups) {
      if (permissionGroup != null && travelledGroups.add(permissionGroup.getName())) {
        this.collectPermissionsInto(target, permissionGroup, taskGroups);
        this.collectAllGroupPermissionsInto(target, this.getGroupsOf(permissionGroup), taskGroups, travelledGroups);
      }
    }
  }

  protected void collectPermissionsInto(@NotNull Collection<Permission> permissions, @NotNull IPermissible permissible,
    @Nullable String[] groups) {
    permissions.addAll(permissible.getPermissions());
    if (groups != null) {
      for (var group : groups) {
        permissions.addAll(permissible.getGroupPermissions().getOrDefault(group, Collections.emptySet()));
      }
    }
  }

  @Override
  public @NotNull Collection<Permission> getAllPermissions(@NotNull IPermissible permissible) {
    return this.getAllGroupPermissions(permissible, null);
  }

  @Override
  public @NotNull Collection<Permission> getAllGroupPermissions(@NotNull IPermissible permissible, String group) {
    Collection<Permission> permissions = new ArrayList<>(permissible.getPermissions());
    if (group != null && permissible.getGroupPermissions().containsKey(group)) {
      permissions.addAll(permissible.getGroupPermissions().get(group));
    }
    for (var permissionGroup : this.getGroupsOf(permissible)) {
      permissions.addAll(this.getAllGroupPermissions(permissionGroup, group));
    }
    return permissions;
  }

  @Override
  public PermissionGroup modifyGroup(@NotNull String name, @NotNull Consumer<PermissionGroup> modifier) {
    var group = this.getGroup(name);

    if (group != null) {
      modifier.accept(group);
      this.updateGroup(group);
    }

    return group;
  }

  @Override
  public PermissionUser modifyUser(@NotNull UUID uniqueId, @NotNull Consumer<PermissionUser> modifier) {
    var user = this.getUser(uniqueId);

    if (user != null) {
      modifier.accept(user);
      this.updateUser(user);
    }

    return user;
  }

  @Override
  public @NotNull List<PermissionUser> modifyUsers(@NotNull String name, @NotNull Consumer<PermissionUser> modifier) {
    var users = this.getUsersByName(name);

    for (var user : users) {
      modifier.accept(user);
      this.updateUser(user);
    }

    return users;
  }
}
