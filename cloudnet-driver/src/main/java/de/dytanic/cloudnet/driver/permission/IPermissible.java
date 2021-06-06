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

import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.document.gson.IJsonDocPropertyable;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IPermissible extends INameable, IJsonDocPropertyable, Comparable<IPermissible>, SerializableObject {

  Collection<String> getGroupNames();

  /**
   * Sets the name of this permissible.
   * <p>
   * An update via {@link IPermissionManagement#updateGroup(IPermissionGroup)} or {@link
   * IPermissionManagement#updateGroup(IPermissionGroup)} is required.
   *
   * @param name the new name
   */
  void setName(@NotNull String name);

  /**
   * Gets the potency of this permissible. If this permissible is an {@link IPermissionGroup}, {@link
   * IPermissionManagement#getHighestPermissionGroup(IPermissionUser)} is sorted by the potency. If this permissible is
   * an {@link IPermissionUser}, in CloudNet it has no specific meaning, but of course you can use it for whatever you
   * want.
   *
   * @return the potency of this permissible
   */
  int getPotency();

  /**
   * Sets the potency of this permissible.
   * <p>
   * An update via {@link IPermissionManagement#updateGroup(IPermissionGroup)} or {@link
   * IPermissionManagement#updateGroup(IPermissionGroup)} is required.
   *
   * @param potency the new potency
   */
  void setPotency(int potency);

  /**
   * Adds a new permission to this permissible and updates it if a permission with that name already exists.
   * <p>
   * An update via {@link IPermissionManagement#updateGroup(IPermissionGroup)} or {@link
   * IPermissionManagement#updateGroup(IPermissionGroup)} is required.
   *
   * @param permission the permission
   * @return {@code true} if the permission has been added successfully or {@code false} if the given {@code permission}
   * was null
   */
  boolean addPermission(@NotNull Permission permission);

  /**
   * Adds a new permission to this permissible and updates it if a permission with that name already exists. This
   * permission will be only effective on servers which have the specified group.
   * <p>
   * An update via {@link IPermissionManagement#updateGroup(IPermissionGroup)} or {@link
   * IPermissionManagement#updateGroup(IPermissionGroup)} is required.
   *
   * @param group      the group where this permission should be effective
   * @param permission the permission
   * @return {@code true} if the permission has been added successfully or {@code false} if the given {@code permission}
   * was null
   */
  boolean addPermission(@NotNull String group, @NotNull Permission permission);

  /**
   * Removes a permission out of this permissible.
   * <p>
   * An update via {@link IPermissionManagement#updateGroup(IPermissionGroup)} or {@link
   * IPermissionManagement#updateGroup(IPermissionGroup)} is required.
   *
   * @param permission the permission
   * @return {@code true} if the permission has been removed successfully or {@code false} if the given {@code
   * permission} doesn't exist
   */
  boolean removePermission(@NotNull String permission);

  /**
   * Removes a permission for a specific group out of this permissible.
   * <p>
   * An update via {@link IPermissionManagement#updateGroup(IPermissionGroup)} or {@link
   * IPermissionManagement#updateGroup(IPermissionGroup)} is required.
   *
   * @param group      the group where this permission is effective
   * @param permission the permission
   * @return {@code true} if the permission has been removed successfully or {@code false} if the given {@code
   * permission} doesn't exist
   */
  boolean removePermission(@NotNull String group, @NotNull String permission);

  /**
   * Gets all effective global permissions. Permissions which are only effective on specific groups are not included.
   *
   * @return a mutable list of all permissions
   */
  Collection<Permission> getPermissions();

  /**
   * Gets all effective permissions on a specific group. Global permissions are not included.
   *
   * @return a mutable map containing mutable lists of permissions
   */
  Map<String, Collection<Permission>> getGroupPermissions();

  /**
   * Gets a permission of this permissible by its name.
   *
   * @param name the case-insensitive name of the permission
   * @return the {@link Permission} if the permission exists or {@code null} if the permission doesn't exist in this
   * permissible or the name is null
   */
  @Nullable
  default Permission getPermission(String name) {
    if (name == null) {
      return null;
    }

    return this.getPermissions().stream().filter(permission -> permission.getName().equalsIgnoreCase(name)).findFirst()
      .orElse(null);
  }

  /**
   * Checks if a permission exists in this permissible by its name.
   *
   * @param name the case-insensitive name of the permission
   * @return {@code true} if the permission exists or {@code false} if the permission doesn't exist in this permissible
   * or this name is null
   */
  default boolean isPermissionSet(@NotNull String name) {
    return this.getPermissions().stream().anyMatch(permission -> permission.getName().equalsIgnoreCase(name));
  }

  /**
   * Adds a new permission to this permissible and updates it if a permission with that name already exists.
   * <p>
   * An update via {@link IPermissionManagement#updateGroup(IPermissionGroup)} or {@link
   * IPermissionManagement#updateGroup(IPermissionGroup)} is required.
   * <p>
   * Equivalent to {@code #addPermission(permission, 0)}
   *
   * @param permission the permission
   * @return {@code true} if the permission has been added successfully or {@code false} if the given {@code permission}
   * was null
   */
  default boolean addPermission(@NotNull String permission) {
    return this.addPermission(permission, 0);
  }

  /**
   * Adds a new permission to this permissible and updates it if a permission with that name already exists.
   * <p>
   * An update via {@link IPermissionManagement#updateGroup(IPermissionGroup)} or {@link
   * IPermissionManagement#updateGroup(IPermissionGroup)} is required.
   * <p>
   * Equivalent to {@code #addPermission(permission, value ? 1 : -1)}
   *
   * @param permission the permission
   * @param value      whether this permission should be applied or not
   * @return {@code true} if the permission has been added successfully or {@code false} if the given {@code permission}
   * was null
   */
  default boolean addPermission(@NotNull String permission, boolean value) {
    return this.addPermission(new Permission(permission, value ? 1 : -1));
  }

  /**
   * Adds a new permission to this permissible and updates it if a permission with that name already exists.
   * <p>
   * An update via {@link IPermissionManagement#updateGroup(IPermissionGroup)} or {@link
   * IPermissionManagement#updateGroup(IPermissionGroup)} is required.
   * <p>
   * Equivalent to {@code #addPermission(new Permission(permission, potency))}
   *
   * @param permission the permission
   * @param potency    the potency of the permission
   * @return {@code true} if the permission has been added successfully or {@code false} if the given {@code permission}
   * was null
   */
  default boolean addPermission(@NotNull String permission, int potency) {
    return this.addPermission(new Permission(permission, potency));
  }

  /**
   * Adds a new permission to this permissible and updates it if a permission with that name already exists.
   * <p>
   * An update via {@link IPermissionManagement#updateGroup(IPermissionGroup)} or {@link
   * IPermissionManagement#updateGroup(IPermissionGroup)} is required.
   * <p>
   * Equivalent to {@code #addPermission(group, permission, 0)}
   *
   * @param group      the group where this permission should be effective
   * @param permission the permission
   * @return {@code true} if the permission has been added successfully or {@code false} if the given {@code permission}
   * was null
   */
  default boolean addPermission(@NotNull String group, @NotNull String permission) {
    return this.addPermission(group, permission, 0);
  }

  /**
   * Adds a new permission to this permissible and updates it if a permission with that name already exists.
   * <p>
   * An update via {@link IPermissionManagement#updateGroup(IPermissionGroup)} or {@link
   * IPermissionManagement#updateGroup(IPermissionGroup)} is required.
   * <p>
   * Equivalent to {@code #addPermission(group, permission, 0)}
   *
   * @param group      the group where this permission should be effective
   * @param permission the permission
   * @param potency    the potency of the permission
   * @return {@code true} if the permission has been added successfully or {@code false} if the given {@code permission}
   * was null
   */
  default boolean addPermission(@NotNull String group, @NotNull String permission, int potency) {
    return this.addPermission(group, new Permission(permission, potency));
  }

  /**
   * Adds a new permission to this permissible and updates it if a permission with that name already exists.
   * <p>
   * An update via {@link IPermissionManagement#updateGroup(IPermissionGroup)} or {@link
   * IPermissionManagement#updateGroup(IPermissionGroup)} is required.
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
  default boolean addPermission(@NotNull String group, @NotNull String permission, int potency, long time,
    TimeUnit unit) {
    return this
      .addPermission(group, new Permission(permission, potency, (System.currentTimeMillis() + unit.toMillis(time))));
  }

  /**
   * Gets a list of the names of all global permissions of this permissible. Modifications of this list are useless.
   *
   * @return a mutable list of all names of the permissions
   */
  default Collection<String> getPermissionNames() {
    return this.getPermissions().stream().map(Permission::getName).collect(Collectors.toList());
  }

  /**
   * Checks whether the given {@code permission} is allowed in the given list of permissions.
   *
   * @param permissions the list of available permissions
   * @param permission  the permission to check
   * @return the result of this check
   */
  default PermissionCheckResult hasPermission(@NotNull Collection<Permission> permissions,
    @NotNull Permission permission) {
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
  default @Nullable Permission findMatchingPermission(@NotNull Collection<Permission> permissions,
    @NotNull Permission permission) {
    return CloudNetDriver.getInstance().getPermissionManagement().findHighestPermission(permissions, permission);
  }

  /**
   * Checks whether the given {@code permission} is allowed in the list of permissions for the specified group in this
   * permissible.
   *
   * @param group      the group to get the available permissions from
   * @param permission the permission to check for
   * @return the result of this check
   */
  default PermissionCheckResult hasPermission(@NotNull String group, @NotNull Permission permission) {
    return this.getGroupPermissions().containsKey(group) ? this
      .hasPermission(this.getGroupPermissions().get(group), permission) : PermissionCheckResult.DENIED;
  }

  /**
   * Checks whether the given {@code permission} is allowed in the list of global permissions in this permissible.
   *
   * @param permission the permission to check for
   * @return the result of this check
   */
  default PermissionCheckResult hasPermission(@NotNull Permission permission) {
    return this.hasPermission(this.getPermissions(), permission);
  }

  /**
   * Checks whether the given {@code permission} is allowed in the list of global permissions in this permissible.
   * <p>
   * Equivalent to {@code #hasPermission(new Permission(permission, 0)}
   *
   * @param permission the permission to check for
   * @return the result of this check
   */
  default PermissionCheckResult hasPermission(@NotNull String permission) {
    return this.hasPermission(new Permission(permission, 0));
  }

  @Override
  default int compareTo(IPermissible o) {
    return this.getPotency() + o.getPotency();
  }
}
