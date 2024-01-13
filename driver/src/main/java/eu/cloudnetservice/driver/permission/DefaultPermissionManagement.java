/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.permission;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * This abstract default permission management represents the implementation for the permission management. Methods
 * doing the calculation for permissions and their result are already implemented and there is no need to implement
 * those yourself.
 *
 * @see PermissionManagement
 * @since 4.0
 */
public abstract class DefaultPermissionManagement implements PermissionManagement {

  /**
   * Gets the child permission management. The default implementation does not allow a child permission management,
   * therefore it's always null.
   *
   * @return the child management, by default null.
   */
  @Override
  public PermissionManagement childPermissionManagement() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PermissionGroup highestPermissionGroup(@NonNull PermissionUser permissionUser) {
    PermissionGroup permissionGroup = null;

    for (var group : this.groupsOf(permissionUser)) {
      if (permissionGroup == null) {
        permissionGroup = group;
        continue;
      }

      if (permissionGroup.potency() <= group.potency()) {
        permissionGroup = group;
      }
    }

    return permissionGroup != null ? permissionGroup : this.defaultPermissionGroup();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean testPermissionUser(@Nullable PermissionUser permissionUser) {
    if (permissionUser == null) {
      return false;
    }

    return this.testPermissible(permissionUser) || permissionUser.groups().removeIf(
      groupInfo -> groupInfo.timeOutMillis() > 0 && groupInfo.timeOutMillis() < System.currentTimeMillis());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean testPermissible(@Nullable Permissible permissible) {
    if (permissible == null) {
      return false;
    }

    var tester = (Predicate<Permission>) permission -> permission.timeOutMillis() > 0
      && permission.timeOutMillis() < System.currentTimeMillis();

    var result = permissible.permissions().removeIf(tester);
    for (var entry : permissible.groupPermissions().entrySet()) {
      result |= entry.getValue().removeIf(tester);
    }

    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Collection<PermissionGroup> groupsOf(@Nullable Permissible permissible) {
    List<PermissionGroup> permissionGroups = new ArrayList<>();

    if (permissible == null) {
      return permissionGroups;
    }

    for (var group : permissible.groupNames()) {
      var permissionGroup = this.group(group);
      if (permissionGroup != null) {
        permissionGroups.add(permissionGroup);
      }
    }

    if (permissionGroups.isEmpty() && permissible instanceof PermissionUser) {
      permissionGroups.add(this.defaultPermissionGroup());
    }

    return permissionGroups;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull PermissionCheckResult permissionResult(
    @NonNull Permissible permissible,
    @NonNull Permission permission
  ) {
    return PermissionCheckResult.fromPermission(
      this.findHighestPermission(this.collectAllPermissions(permissible, null), permission));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull PermissionCheckResult groupPermissionResult(
    @NonNull Permissible permissible,
    @NonNull String group,
    @NonNull Permission permission
  ) {
    return this.groupsPermissionResult(permissible, new String[]{group}, permission);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull PermissionCheckResult groupsPermissionResult(@NonNull Permissible permissible,
    @NonNull String[] groups,
    @NonNull Permission permission) {
    return PermissionCheckResult
      .fromPermission(this.findHighestPermission(this.collectAllPermissions(permissible, groups), permission));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Nullable
  public Permission findHighestPermission(@NonNull Collection<Permission> permissions, @NonNull Permission permission) {
    Permission lastMatch = null;
    // search for a better match
    for (var permissionEntry : permissions) {
      var used = lastMatch == null ? permission : lastMatch;
      // the "star" permission represents a permission which allows access to every command
      if (permissionEntry.name().equals("*") && permissionEntry.compareTo(used) >= 0) {
        lastMatch = permissionEntry;
        continue;
      }
      // searches for "perm.*"-permissions (allowing all sub permissions of the given permission name start
      if (permissionEntry.name().endsWith("*")
        && permission.name().contains(permissionEntry.name().replace("*", ""))
        && permissionEntry.compareTo(used) >= 0) {
        lastMatch = permissionEntry;
        continue;
      }
      // checks if the current permission is exactly (case-sensitive) the permission for which we are searching
      if (permission.name().equalsIgnoreCase(permissionEntry.name()) && permissionEntry.compareTo(used) >= 0) {
        lastMatch = permissionEntry;
      }
    }

    return lastMatch;
  }

  protected @NonNull Collection<Permission> collectAllPermissions(
    @NonNull Permissible permissible,
    @Nullable String[] groups
  ) {
    return this.collectAllPermissionsTo(new HashSet<>(), permissible, groups);
  }

  /**
   * Collects all permissions for the given permissible into the given target collection. If the groups array is not
   * null group specific permissions are included in the collection.
   *
   * @param target      the target to collect to.
   * @param permissible the permissible to collect for.
   * @param groups      the groups to collect group permissions for.
   * @return all collected permissions.
   * @throws NullPointerException if the given target or permissible is null.
   */
  protected @NonNull Collection<Permission> collectAllPermissionsTo(
    @NonNull Collection<Permission> target,
    @NonNull Permissible permissible,
    @Nullable String[] groups
  ) {
    this.collectPermissionsInto(target, permissible, groups);
    this.collectAllGroupPermissionsInto(target, this.groupsOf(permissible), groups, new HashSet<>());

    return target;
  }

  /**
   * Collects all permissions into the given target collection by travelling recursively through every of the given
   * group and the parents of the groups. If task groups are given the group permissions are included too. The travelled
   * group collection is used to prevent infinite recursion, the collection is passed down while travelling to the next
   * group.
   *
   * @param target          the collection to collect into.
   * @param groups          the groups to collect the permissions for.
   * @param taskGroups      the groups to collect group permissions for.
   * @param travelledGroups all already visited groups.
   * @throws NullPointerException if the given target, group or travelled group collection is null.
   */
  protected void collectAllGroupPermissionsInto(
    @NonNull Collection<Permission> target,
    @NonNull Collection<PermissionGroup> groups,
    @Nullable String[] taskGroups,
    @NonNull Collection<String> travelledGroups
  ) {
    for (var permissionGroup : groups) {
      if (permissionGroup != null && travelledGroups.add(permissionGroup.name())) {
        this.collectPermissionsInto(target, permissionGroup, taskGroups);
        this.collectAllGroupPermissionsInto(target, this.groupsOf(permissionGroup), taskGroups, travelledGroups);
      }
    }
  }

  /**
   * Collects all permissions of the given permissible into the given permission collection. If the groups array is not
   * null group permissions are included too.
   *
   * @param permissions the collection to collect into.
   * @param permissible the permissible to collect for.
   * @param groups      optional target groups to collect for.
   * @throws NullPointerException if the given permissions or permissible is null.
   */
  protected void collectPermissionsInto(
    @NonNull Collection<Permission> permissions,
    @NonNull Permissible permissible,
    @Nullable String[] groups
  ) {
    permissions.addAll(permissible.permissions());
    if (groups != null) {
      for (var group : groups) {
        permissions.addAll(permissible.groupPermissions().getOrDefault(group, Collections.emptySet()));
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Collection<Permission> allPermissions(@NonNull Permissible permissible) {
    return this.allGroupPermissions(permissible, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Collection<Permission> allGroupPermissions(@NonNull Permissible permissible, String group) {
    Collection<Permission> permissions = new ArrayList<>(permissible.permissions());
    if (group != null && permissible.groupPermissions().containsKey(group)) {
      permissions.addAll(permissible.groupPermissions().get(group));
    }
    for (var permissionGroup : this.groupsOf(permissible)) {
      permissions.addAll(this.allGroupPermissions(permissionGroup, group));
    }
    return permissions;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable PermissionGroup modifyGroup(
    @NonNull String name,
    @NonNull BiConsumer<PermissionGroup, PermissionGroup.Builder> modifier
  ) {
    var group = this.group(name);
    if (group != null) {
      var builder = PermissionGroup.builder(group);
      // accept the action
      modifier.accept(group, builder);
      this.updateGroup(builder.build());
    }

    return group;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable PermissionUser modifyUser(
    @NonNull UUID uniqueId,
    @NonNull BiConsumer<PermissionUser, PermissionUser.Builder> modifier
  ) {
    var user = this.user(uniqueId);
    if (user != null) {
      var builder = PermissionUser.builder(user);
      // accept the action
      modifier.accept(user, builder);
      this.updateUser(builder.build());
    }

    return user;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull List<PermissionUser> modifyUsers(
    @NonNull String name,
    @NonNull BiConsumer<PermissionUser, PermissionUser.Builder> modifier
  ) {
    var users = this.usersByName(name);
    for (var user : users) {
      var builder = PermissionUser.builder(user);
      // accept the action
      modifier.accept(user, builder);
      this.updateUser(builder.build());
    }

    return users;
  }
}
