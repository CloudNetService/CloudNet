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

package eu.cloudnetservice.cloudnet.driver.permission;

import eu.cloudnetservice.cloudnet.common.Nameable;
import eu.cloudnetservice.cloudnet.common.document.property.DocPropertyHolder;
import eu.cloudnetservice.cloudnet.driver.CloudNetDriver;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

public interface Permissible extends Nameable, DocPropertyHolder, Comparable<Permissible> {

  @NonNull Collection<String> groupNames();

  /**
   * Gets the potency of this permissible. If this permissible is an {@link PermissionGroup}, {@link
   * PermissionManagement#highestPermissionGroup(PermissionUser)} is sorted by the potency. If this permissible is an
   * {@link PermissionUser}, in CloudNet it has no specific meaning, but of course you can use it for whatever you
   * want.
   *
   * @return the potency of this permissible
   */
  int potency();

  /**
   * Adds a new permission to this permissible and updates it if a permission with that name already exists.
   * <p>
   * An update via {@link PermissionManagement#updateGroup(PermissionGroup)} or {@link
   * PermissionManagement#updateGroup(PermissionGroup)} is required.
   *
   * @param permission the permission
   * @return {@code true} if the permission has been added successfully or {@code false} if the given {@code permission}
   * was null
   */
  boolean addPermission(@NonNull Permission permission);

  /**
   * Adds a new permission to this permissible and updates it if a permission with that name already exists. This
   * permission will be only effective on servers which have the specified group.
   * <p>
   * An update via {@link PermissionManagement#updateGroup(PermissionGroup)} or {@link
   * PermissionManagement#updateGroup(PermissionGroup)} is required.
   *
   * @param group      the group where this permission should be effective
   * @param permission the permission
   * @return {@code true} if the permission has been added successfully or {@code false} if the given {@code permission}
   * was null
   */
  boolean addPermission(@NonNull String group, @NonNull Permission permission);

  /**
   * Removes a permission out of this permissible.
   * <p>
   * An update via {@link PermissionManagement#updateGroup(PermissionGroup)} or {@link
   * PermissionManagement#updateGroup(PermissionGroup)} is required.
   *
   * @param permission the permission
   * @return {@code true} if the permission has been removed successfully or {@code false} if the given {@code
   * permission} doesn't exist
   */
  boolean removePermission(@NonNull String permission);

  /**
   * Removes a permission for a specific group out of this permissible.
   * <p>
   * An update via {@link PermissionManagement#updateGroup(PermissionGroup)} or {@link
   * PermissionManagement#updateGroup(PermissionGroup)} is required.
   *
   * @param group      the group where this permission is effective
   * @param permission the permission
   * @return {@code true} if the permission has been removed successfully or {@code false} if the given {@code
   * permission} doesn't exist
   */
  boolean removePermission(@NonNull String group, @NonNull String permission);

  /**
   * Gets all effective global permissions. Permissions which are only effective on specific groups are not included.
   *
   * @return a mutable list of all permissions
   */
  @NonNull Collection<Permission> permissions();

  /**
   * Gets all effective permissions on a specific group. Global permissions are not included.
   *
   * @return a mutable map containing mutable lists of permissions
   */
  @NonNull Map<String, Set<Permission>> groupPermissions();

  /**
   * Gets a permission of this permissible by its name.
   *
   * @param name the case-insensitive name of the permission
   * @return the {@link Permission} if the permission exists or {@code null} if the permission doesn't exist in this
   * permissible or the name is null
   */
  default @Nullable Permission permission(@Nullable String name) {
    return name == null ? null : this.permissions().stream()
      .filter(permission -> permission.name().equalsIgnoreCase(name))
      .findFirst()
      .orElse(null);
  }

  /**
   * Checks if a permission exists in this permissible by its name.
   *
   * @param name the case-insensitive name of the permission
   * @return {@code true} if the permission exists or {@code false} if the permission doesn't exist in this permissible
   * or this name is null
   */
  default boolean isPermissionSet(@NonNull String name) {
    return this.permissions().stream().anyMatch(permission -> permission.name().equalsIgnoreCase(name));
  }

  /**
   * Adds a new permission to this permissible and updates it if a permission with that name already exists.
   * <p>
   * An update via {@link PermissionManagement#updateGroup(PermissionGroup)} or {@link
   * PermissionManagement#updateGroup(PermissionGroup)} is required.
   * <p>
   * Equivalent to {@code #addPermission(permission, 0)}
   *
   * @param permission the permission
   * @return {@code true} if the permission has been added successfully or {@code false} if the given {@code permission}
   * was null
   */
  default boolean addPermission(@NonNull String permission) {
    return this.addPermission(permission, 0);
  }

  /**
   * Adds a new permission to this permissible and updates it if a permission with that name already exists.
   * <p>
   * An update via {@link PermissionManagement#updateGroup(PermissionGroup)} or {@link
   * PermissionManagement#updateGroup(PermissionGroup)} is required.
   * <p>
   * Equivalent to {@code #addPermission(permission, value ? 1 : -1)}
   *
   * @param permission the permission
   * @param value      whether this permission should be applied or not
   * @return {@code true} if the permission has been added successfully or {@code false} if the given {@code permission}
   * was null
   */
  default boolean addPermission(@NonNull String permission, boolean value) {
    return this.addPermission(Permission.builder().name(permission).potency(value ? 1 : -1).build());
  }

  /**
   * Adds a new permission to this permissible and updates it if a permission with that name already exists.
   * <p>
   * An update via {@link PermissionManagement#updateGroup(PermissionGroup)} or {@link
   * PermissionManagement#updateGroup(PermissionGroup)} is required.
   * <p>
   * Equivalent to {@code #addPermission(new Permission(permission, potency))}
   *
   * @param permission the permission
   * @param potency    the potency of the permission
   * @return {@code true} if the permission has been added successfully or {@code false} if the given {@code permission}
   * was null
   */
  default boolean addPermission(@NonNull String permission, int potency) {
    return this.addPermission(Permission.builder().name(permission).potency(potency).build());
  }

  /**
   * Adds a new permission to this permissible and updates it if a permission with that name already exists.
   * <p>
   * An update via {@link PermissionManagement#updateGroup(PermissionGroup)} or {@link
   * PermissionManagement#updateGroup(PermissionGroup)} is required.
   * <p>
   * Equivalent to {@code #addPermission(group, permission, 0)}
   *
   * @param group      the group where this permission should be effective
   * @param permission the permission
   * @return {@code true} if the permission has been added successfully or {@code false} if the given {@code permission}
   * was null
   */
  default boolean addPermission(@NonNull String group, @NonNull String permission) {
    return this.addPermission(group, permission, 0);
  }

  /**
   * Adds a new permission to this permissible and updates it if a permission with that name already exists.
   * <p>
   * An update via {@link PermissionManagement#updateGroup(PermissionGroup)} or {@link
   * PermissionManagement#updateGroup(PermissionGroup)} is required.
   * <p>
   * Equivalent to {@code #addPermission(group, permission, 0)}
   *
   * @param group      the group where this permission should be effective
   * @param permission the permission
   * @param potency    the potency of the permission
   * @return {@code true} if the permission has been added successfully or {@code false} if the given {@code permission}
   * was null
   */
  default boolean addPermission(@NonNull String group, @NonNull String permission, int potency) {
    return this.addPermission(group, Permission.builder().name(permission).potency(potency).build());
  }

  /**
   * Adds a new permission to this permissible and updates it if a permission with that name already exists.
   * <p>
   * An update via {@link PermissionManagement#updateGroup(PermissionGroup)} or {@link
   * PermissionManagement#updateGroup(PermissionGroup)} is required.
   * <p>
   * Equivalent to {@code #addPermission(group, new Permission(permission, potency, (System.currentTimeMillis() +
   * millis.toMillis(time))))}
   *
   * @param group      the group where this permission should be effective
   * @param permission the permission
   * @param potency    the potency of the permission
   * @param time       the time when this permission should expire
   * @param unit       the time unit of the {@code time} param
   * @return {@code true} if the permission has been added successfully or {@code false} if the given {@code permission}
   * was null
   */
  default boolean addPermission(
    @NonNull String group,
    @NonNull String permission,
    int potency,
    long time,
    @NonNull TimeUnit unit
  ) {
    return this.addPermission(group, new Permission(
      permission,
      potency,
      System.currentTimeMillis() + unit.toMillis(time)));
  }

  /**
   * Gets a list of the names of all global permissions of this permissible. Modifications to this list are not
   * possible.
   *
   * @return a mutable list of all names of the permissions
   */
  default @Unmodifiable Collection<String> permissionNames() {
    return this.permissions().stream()
      .map(Permission::name)
      .toList();
  }

  /**
   * Checks whether the given {@code permission} is allowed in the given list of permissions.
   *
   * @param permissions the list of available permissions
   * @param permission  the permission to check
   * @return the result of this check
   */
  default @NonNull PermissionCheckResult hasPermission(
    @NonNull Collection<Permission> permissions,
    @NonNull Permission permission
  ) {
    return PermissionCheckResult.fromPermission(this.findMatchingPermission(permissions, permission));
  }

  /**
   * Finds the best matching permission in the given {@code permissions} by logically checking the absolute potency
   * against each other to find the permission with the highest positive or negative potency.
   *
   * @param permissions the permission to check for.
   * @param permission  the initial permission to check against.
   * @return the logic permission which has the highest potency.
   */
  default @Nullable Permission findMatchingPermission(
    @NonNull Collection<Permission> permissions,
    @NonNull Permission permission
  ) {
    return CloudNetDriver.instance().permissionManagement().findHighestPermission(permissions, permission);
  }

  /**
   * Checks whether the given {@code permission} is allowed in the list of permissions for the specified group in this
   * permissible.
   *
   * @param group      the group to get the available permissions from
   * @param permission the permission to check for
   * @return the result of this check
   */
  default @NonNull PermissionCheckResult hasPermission(@NonNull String group, @NonNull Permission permission) {
    return this.groupPermissions().containsKey(group)
      ? this.hasPermission(this.groupPermissions().get(group), permission)
      : PermissionCheckResult.DENIED;
  }

  /**
   * Checks whether the given {@code permission} is allowed in the list of global permissions in this permissible.
   *
   * @param permission the permission to check for
   * @return the result of this check
   */
  default @NonNull PermissionCheckResult hasPermission(@NonNull Permission permission) {
    return this.hasPermission(this.permissions(), permission);
  }

  /**
   * Checks whether the given {@code permission} is allowed in the list of global permissions in this permissible.
   * <p>
   * Equivalent to {@code #hasPermission(new Permission(permission, 0)}
   *
   * @param permission the permission to check for
   * @return the result of this check
   */
  default @NonNull PermissionCheckResult hasPermission(@NonNull String permission) {
    return this.hasPermission(Permission.of(permission));
  }

  @Override
  default int compareTo(@NonNull Permissible o) {
    return this.potency() + o.potency();
  }
}
