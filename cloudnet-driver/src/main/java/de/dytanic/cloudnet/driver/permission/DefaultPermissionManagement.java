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

import com.google.common.collect.Iterables;
import de.dytanic.cloudnet.common.concurrent.ITask;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.jetbrains.annotations.ApiStatus;
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

  @Deprecated
  public List<IPermissionUser> getUser(String name) {
    return this.getUsers(name);
  }

  @Deprecated
  public Collection<IPermissionUser> getUserByGroup(String group) {
    return this.getUsersByGroup(group);
  }

  @Override
  public IPermissionGroup getHighestPermissionGroup(@NotNull IPermissionUser permissionUser) {
    IPermissionGroup permissionGroup = null;

    for (IPermissionGroup group : this.getGroups(permissionUser)) {
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
  public boolean testPermissionGroup(@Nullable IPermissionGroup permissionGroup) {
    return this.testPermissible(permissionGroup);
  }

  @Override
  public boolean testPermissionUser(@Nullable IPermissionUser permissionUser) {
    if (permissionUser == null) {
      return false;
    }

    return this.testPermissible(permissionUser) ||
      permissionUser.getGroups().removeIf(
        groupInfo -> groupInfo.getTimeOutMillis() > 0 && groupInfo.getTimeOutMillis() < System.currentTimeMillis());
  }

  @Override
  public boolean testPermissible(@Nullable IPermissible permissible) {
    if (permissible == null) {
      return false;
    }

    Predicate<Permission> tester = permission -> permission.getTimeOutMillis() > 0
      && permission.getTimeOutMillis() < System.currentTimeMillis();

    boolean result = permissible.getPermissions().removeIf(tester);

    for (Map.Entry<String, Collection<Permission>> entry : permissible.getGroupPermissions().entrySet()) {
      result = result || entry.getValue().removeIf(tester);
    }

    return result;
  }

  @Override
  @NotNull
  public Collection<IPermissionGroup> getGroups(@Nullable IPermissible permissible) {
    List<IPermissionGroup> permissionGroups = new ArrayList<>();

    if (permissible == null) {
      return permissionGroups;
    }

    for (String group : permissible.getGroupNames()) {
      IPermissionGroup permissionGroup = this.getGroup(group);

      if (permissionGroup != null) {
        permissionGroups.add(permissionGroup);
      }
    }

    if (permissionGroups.isEmpty() && permissible instanceof IPermissionUser) {
      permissionGroups.add(this.getDefaultPermissionGroup());
    }

    return permissionGroups;
  }

  @Override
  public Collection<IPermissionGroup> getExtendedGroups(@Nullable IPermissionGroup group) {
    return this.getGroups(group);
  }

  @Override
  @NotNull
  public PermissionCheckResult getPermissionResult(@NotNull IPermissible permissible, @NotNull String permission) {
    return this.getPermissionResult(permissible, new Permission(permission));
  }

  @Override
  @NotNull
  public PermissionCheckResult getPermissionResult(@NotNull IPermissible permissible, @NotNull Permission permission) {
    return PermissionCheckResult
      .fromPermission(this.findHighestPermission(this.collectAllPermissions(permissible, null), permission));
  }

  @Override
  @NotNull
  public PermissionCheckResult getPermissionResult(@NotNull IPermissible permissible, @NotNull String group,
    @NotNull Permission permission) {
    return this.getPermissionResult(permissible, Collections.singleton(group), permission);
  }

  @Override
  public @NotNull PermissionCheckResult getPermissionResult(@NotNull IPermissible permissible,
    @NotNull Iterable<String> groups, @NotNull Permission permission) {
    return this.getPermissionResult(permissible, Iterables.toArray(groups, String.class), permission);
  }

  @Override
  public @NotNull PermissionCheckResult getPermissionResult(@NotNull IPermissible permissible, @NotNull String[] groups,
    @NotNull Permission permission) {
    return PermissionCheckResult
      .fromPermission(this.findHighestPermission(this.collectAllPermissions(permissible, groups), permission));
  }

  @Override
  @Nullable
  public Permission findHighestPermission(@NotNull Collection<Permission> permissions, @NotNull Permission permission) {
    Permission lastMatch = null;
    // search for a better match
    for (Permission permissionEntry : permissions) {
      Permission used = lastMatch == null ? permission : lastMatch;
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
    this.collectAllGroupPermissionsInto(target, this.getGroups(permissible), groups, new HashSet<>());

    return target;
  }

  protected void collectAllGroupPermissionsInto(@NotNull Collection<Permission> target,
    @NotNull Collection<IPermissionGroup> groups,
    @Nullable String[] taskGroups, @NotNull Collection<String> travelledGroups) {
    for (IPermissionGroup permissionGroup : groups) {
      if (permissionGroup != null && travelledGroups.add(permissionGroup.getName())) {
        this.collectPermissionsInto(target, permissionGroup, taskGroups);
        this.collectAllGroupPermissionsInto(target, this.getGroups(permissionGroup), taskGroups, travelledGroups);
      }
    }
  }

  protected void collectPermissionsInto(@NotNull Collection<Permission> permissions, @NotNull IPermissible permissible,
    @Nullable String[] groups) {
    permissions.addAll(permissible.getPermissions());
    if (groups != null) {
      for (String group : groups) {
        permissions.addAll(permissible.getGroupPermissions().getOrDefault(group, Collections.emptySet()));
      }
    }
  }

  /**
   * @deprecated has no use internally anymore, will be removed in a further release.
   */
  @Deprecated
  @ApiStatus.ScheduledForRemoval
  public PermissionCheckResult getPermissionResult(IPermissible permissible,
    Supplier<PermissionCheckResult> permissionTester,
    Function<IPermissionGroup, PermissionCheckResult> extendedGroupsTester) {
    switch (permissionTester.get()) {
      case ALLOWED:
        return PermissionCheckResult.ALLOWED;
      case FORBIDDEN:
        return PermissionCheckResult.FORBIDDEN;
      default:
        for (IPermissionGroup permissionGroup : this.getGroups(permissible)) {
          if (permissionGroup != null) {
            PermissionCheckResult result = extendedGroupsTester.apply(permissionGroup);
            if (result == PermissionCheckResult.ALLOWED || result == PermissionCheckResult.FORBIDDEN) {
              return result;
            }
          }
        }
        break;
    }

    IPermissionGroup publicGroup = this.getDefaultPermissionGroup();
    return publicGroup != null ? extendedGroupsTester.apply(publicGroup) : PermissionCheckResult.DENIED;
  }

  /**
   * @deprecated has no use internally anymore, will be removed in a further release.
   */
  @Deprecated
  @ApiStatus.ScheduledForRemoval
  public PermissionCheckResult tryExtendedGroups(@NotNull String firstGroup, @Nullable IPermissionGroup permissionGroup,
    @NotNull Permission permission, int layer) {
    if (permissionGroup == null) {
      return PermissionCheckResult.DENIED;
    }
    if (layer >= 30) {
      System.err.println("Detected recursive permission group implementation on group " + firstGroup);
      return PermissionCheckResult.DENIED;
    }
    layer++;

    switch (permissionGroup.hasPermission(permission)) {
      case ALLOWED:
        return PermissionCheckResult.ALLOWED;
      case FORBIDDEN:
        return PermissionCheckResult.FORBIDDEN;
      default:
        for (IPermissionGroup extended : this.getGroups(permissionGroup)) {
          PermissionCheckResult result = this.tryExtendedGroups(firstGroup, extended, permission, layer);
          if (result == PermissionCheckResult.ALLOWED || result == PermissionCheckResult.FORBIDDEN) {
            return result;
          }
        }
        break;
    }

    return PermissionCheckResult.DENIED;
  }

  /**
   * @deprecated has no use internally anymore, will be removed in a further release.
   */
  @Deprecated
  @ApiStatus.ScheduledForRemoval
  public PermissionCheckResult tryExtendedGroups(@NotNull String firstGroup, @Nullable IPermissionGroup permissionGroup,
    @NotNull String group, @NotNull Permission permission, int layer) {
    if (permissionGroup == null) {
      return PermissionCheckResult.DENIED;
    }
    if (layer >= 30) {
      System.err.println("Detected recursive permission group implementation on group " + firstGroup);
      return PermissionCheckResult.DENIED;
    }
    layer++;

    switch (permissionGroup.hasPermission(group, permission)) {
      case ALLOWED:
        return PermissionCheckResult.ALLOWED;
      case FORBIDDEN:
        return PermissionCheckResult.FORBIDDEN;
      default:
        for (IPermissionGroup extended : this.getExtendedGroups(permissionGroup)) {
          PermissionCheckResult result = this.tryExtendedGroups(firstGroup, extended, group, permission, layer);
          if (result == PermissionCheckResult.ALLOWED || result == PermissionCheckResult.FORBIDDEN) {
            return result;
          }
        }
        break;
    }

    return PermissionCheckResult.DENIED;
  }

  @Override
  public @NotNull Collection<Permission> getAllPermissions(@NotNull IPermissible permissible) {
    return this.getAllPermissions(permissible, null);
  }

  @Override
  public @NotNull Collection<Permission> getAllPermissions(@NotNull IPermissible permissible, String group) {
    Collection<Permission> permissions = new ArrayList<>(permissible.getPermissions());
    if (group != null && permissible.getGroupPermissions().containsKey(group)) {
      permissions.addAll(permissible.getGroupPermissions().get(group));
    }
    for (IPermissionGroup permissionGroup : this.getGroups(permissible)) {
      permissions.addAll(this.getAllPermissions(permissionGroup, group));
    }
    return permissions;
  }

  @Override
  public @NotNull ITask<IPermissionGroup> modifyGroupAsync(@NotNull String name,
    @NotNull Consumer<IPermissionGroup> modifier) {
    ITask<IPermissionGroup> task = this.getGroupAsync(name);

    task.onComplete(group -> {
      if (group != null) {
        modifier.accept(group);
        this.updateGroup(group);
      }
    });

    return task;
  }

  @Override
  public @NotNull ITask<IPermissionUser> modifyUserAsync(@NotNull UUID uniqueId,
    @NotNull Consumer<IPermissionUser> modifier) {
    ITask<IPermissionUser> task = this.getUserAsync(uniqueId);

    task.onComplete(user -> {
      if (user != null) {
        modifier.accept(user);
        this.updateUser(user);
      }
    });

    return task;
  }

  @Override
  public @NotNull ITask<List<IPermissionUser>> modifyUsersAsync(@NotNull String name,
    @NotNull Consumer<IPermissionUser> modifier) {
    ITask<List<IPermissionUser>> task = this.getUsersAsync(name);

    task.onComplete(users -> {
      for (IPermissionUser user : users) {
        modifier.accept(user);
        this.updateUser(user);
      }
    });

    return task;
  }
}
