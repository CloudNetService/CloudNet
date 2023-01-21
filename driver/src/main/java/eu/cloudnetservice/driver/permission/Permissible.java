/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

import eu.cloudnetservice.common.Nameable;
import eu.cloudnetservice.common.document.property.DocPropertyHolder;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

/**
 * The permissible represents the base for any permission holding object. The permissible is mutable, but any changes to
 * the permissible itself do not have any effect without updating the permissible using the permission management.
 *
 * @since 4.0
 */
public interface Permissible extends Nameable, DocPropertyHolder, Comparable<Permissible> {

  /**
   * Gets the potency of this permissible. The potency is used for comparison of permissibles. A higher potency results
   * in higher significance.
   *
   * @return the potency of this permissible.
   * @see Permissible#compareTo(Permissible)
   */
  int potency();

  /**
   * Removes the given permission from this permissible. This only removes global permissions, not group specific ones.
   * <p>
   * After removing the permission an update is required.
   *
   * @param permission the permission to remove.
   * @return true if any permission was removed, false otherwise.
   * @throws NullPointerException if the given permission is null.
   */
  boolean removePermission(@NonNull String permission);

  /**
   * Removes the given permission from this permissible and the given target group. This only removes group specific
   * permissions, not global ones.
   * <p>
   * After removing the permission an update is required.
   *
   * @param permission the permission to remove.
   * @param group      the target group to remove the permission from.
   * @return true if any permission was removed, false otherwise.
   * @throws NullPointerException if the given group or permission is null.
   */
  boolean removePermission(@NonNull String permission, @NonNull String group);

  /**
   * Gets all global permissions of this permissible. Specific group permissions are not included.
   *
   * @return all global permissions.
   */
  @NonNull Collection<Permission> permissions();

  /**
   * Gets all group specific permissions. Global permissions are not included.
   *
   * @return all group specific permissions.
   */
  @NonNull Map<String, Set<Permission>> groupPermissions();

  /**
   * Collects all group names of this permissible.
   *
   * @return all groups names.
   */
  @NonNull Collection<String> groupNames();

  /**
   * Gets a permission of this permissible by its name.
   *
   * @param name the name of the permission.
   * @return the permission with the given name or null if the name is null or no permission was found.
   */
  default @Nullable Permission permission(@Nullable String name) {
    return name == null ? null : this.permissions().stream()
      .filter(permission -> permission.name().equalsIgnoreCase(name))
      .findFirst()
      .orElse(null);
  }

  /**
   * Checks if the permission has the given permission.
   *
   * @param name the case-insensitive name of the permission.
   * @return true if the permissible has the given permission, false otherwise.
   * @throws NullPointerException if the given name is null.
   */
  default boolean isPermissionSet(@NonNull String name) {
    return this.permissions().stream().anyMatch(permission -> permission.name().equalsIgnoreCase(name));
  }

  /**
   * Gets the creation time as unix timestamp of this permissible.
   *
   * @return the creation time of this permissible.
   */
  long creationTime();

  /**
   * Adds the given permission to this permissible. If a permission with the same name already exists the permission is
   * replaced with the given one.
   * <p>
   * After adding the permission an update is required.
   *
   * @param permission the permission to add.
   * @throws NullPointerException if the given permission is null.
   */
  void addPermission(@NonNull Permission permission);

  /**
   * Adds the given permission with a potency of 0 to this permissible. If a permission with the same name already
   * exists the permission is replaced with the given one.
   * <p>
   * After adding the permission an update is required.
   *
   * @param permission the permission to add.
   * @throws NullPointerException if the given permission is null.
   */
  default void addPermission(@NonNull String permission) {
    this.addPermission(Permission.of(permission));
  }

  /**
   * Adds the given permission to this permissible for the specified target group. If a permission with the same name
   * already exists the permission is replaced with the given one.
   * <p>
   * After adding the permission an update is required.
   *
   * @param group      the group to add the permission for.
   * @param permission the permission to add.
   * @throws NullPointerException if the given group or permission is null.
   */
  void addPermission(@NonNull String group, @NonNull Permission permission);

  /**
   * Adds the given permission to this permissible for the specified target group, the potency is set to 0. If a
   * permission with the same name already exists the permission is replaced with the given one.
   * <p>
   * After adding the permission an update is required.
   *
   * @param group      the group to add the permission for.
   * @param permission the permission to add.
   * @throws NullPointerException if the given group or permission is null.
   */
  default void addPermission(@NonNull String group, @NonNull String permission) {
    this.addPermission(group, Permission.of(permission));
  }

  /**
   * Gets an unmodifiable collection of all global permission names.
   *
   * @return all global permissions
   */
  default @Unmodifiable @NonNull Collection<String> permissionNames() {
    return this.permissions().stream().map(Nameable::name).toList();
  }

  /**
   * Checks if the collection of given permissions contains a permission that matches the given permission.
   *
   * @param permissions the permissions to search in.
   * @param permission  the permission to check for.
   * @return the result of the permission search.
   * @throws NullPointerException if the given permissions or permission is null.
   */
  default @NonNull PermissionCheckResult hasPermission(
    @NonNull Collection<Permission> permissions,
    @NonNull Permission permission
  ) {
    return PermissionCheckResult.fromPermission(this.findMatchingPermission(permissions, permission));
  }

  /**
   * Finds the best matching permission in the given permissions by logically checking the absolute potency against each
   * other to find the permission with the highest positive or negative potency.
   *
   * @param permissions the permission to check for.
   * @param permission  the initial permission to check against.
   * @return the logic permission which has the highest potency, null if none matches.
   * @throws NullPointerException if the given permissions or permission is null.
   * @see PermissionManagement#findHighestPermission(Collection, Permission)
   */
  default @Nullable Permission findMatchingPermission(
    @NonNull Collection<Permission> permissions,
    @NonNull Permission permission
  ) {
    var permissionManagement = InjectionLayer.boot().instance(PermissionManagement.class);
    return permissionManagement.findHighestPermission(permissions, permission);
  }

  /**
   * Checks if this permissible has the given permission for the specified target group. Only group permissions are
   * included in this check, global permissions don't have any effect.
   *
   * @param group      the target group of the permission.
   * @param permission the permission to check for.
   * @return the result of the permission check.
   * @throws NullPointerException if the given group or permission is null.
   */
  default @NonNull PermissionCheckResult hasPermission(@NonNull String group, @NonNull Permission permission) {
    return this.groupPermissions().containsKey(group)
      ? this.hasPermission(this.groupPermissions().get(group), permission)
      : PermissionCheckResult.DENIED;
  }

  /**
   * Checks if this permissible has the given permission. Only global permissions are included in this check.
   *
   * @param permission the permission to check for.
   * @return the result of the permission check.
   * @throws NullPointerException if the given permission is null.
   */
  default @NonNull PermissionCheckResult hasPermission(@NonNull Permission permission) {
    return this.hasPermission(this.permissions(), permission);
  }

  /**
   * Checks if this permissible has the given permission with a potency of 0. Only global permissions are included in
   * this check.
   *
   * @param permission the permission to check for.
   * @return the result of the permission check.
   * @throws NullPointerException if the given permission is null.
   */
  default @NonNull PermissionCheckResult hasPermission(@NonNull String permission) {
    return this.hasPermission(Permission.of(permission));
  }

  @Override
  default int compareTo(@NonNull Permissible o) {
    return this.potency() + o.potency();
  }
}
