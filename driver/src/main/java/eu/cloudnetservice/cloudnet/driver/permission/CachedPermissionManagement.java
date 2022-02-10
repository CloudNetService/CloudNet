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

import java.util.Map;
import java.util.UUID;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The cached permission management does not differ that much from the permission management. This management is
 * responsible for the caching and removing of cached permission users and permission groups while also allowing
 * permanent caching if desired.
 *
 * @see eu.cloudnetservice.cloudnet.driver.permission.PermissionManagement
 * @since 4.0
 */
public interface CachedPermissionManagement extends PermissionManagement {

  /**
   * Gets a map with all cached permission users mapped by their uuid.
   *
   * @return all cached permission users.
   */
  @NonNull Map<UUID, PermissionUser> cachedPermissionUsers();

  /**
   * Gets a map with all cached permission groups mapped by their name.
   *
   * @return all cached permission groups.
   */
  @NonNull Map<String, PermissionGroup> cachedPermissionGroups();

  /**
   * Searches the permission user cache for a permission user with the given unique id.
   *
   * @param uniqueId the unique id associated with the permission user.
   * @return the cached permission user for the given unique id, null if no user was found.
   * @throws NullPointerException if the given unique id is null.
   */
  @Nullable PermissionUser cachedUser(@NonNull UUID uniqueId);

  /**
   * Searches the permission group cache for a permission group with the given name.
   *
   * @param name the name associated with the permission group.
   * @return the cached permission group for the given name, null if no group was found.
   * @throws NullPointerException if the given name is null.
   */
  @Nullable PermissionGroup cachedGroup(@NonNull String name);

  /**
   * Acquires a lock for the permission user. The lock ensures that the user stays cached.
   * <p>
   * Each method call results in a new lock for the user.
   *
   * @param user the user to acquire the lock for.
   * @throws NullPointerException if the given user is null.
   */
  void acquireLock(@NonNull PermissionUser user);

  /**
   * Acquires a lock for the permission group. The lock ensures that the group stays cached.
   * <p>
   * Each method call results in a new lock for the group.
   *
   * @param group the group to acquire the lock for.
   * @throws NullPointerException if the given group is null.
   */
  void acquireLock(@NonNull PermissionGroup group);

  /**
   * Checks if the given user has any locks locking the user in the cache.
   *
   * @param user the user to check for locks.
   * @return true if there are no locks for the given user, false otherwise.
   * @throws NullPointerException if the given user is null.
   */
  boolean locked(@NonNull PermissionUser user);

  /**
   * Checks if the given group has any locks locking the group in the cache.
   *
   * @param group the group to check for locks.
   * @return true if there are no locks for the given group, false otherwise.
   * @throws NullPointerException if the given group is null.
   */
  boolean locked(@NonNull PermissionGroup group);

  /**
   * Removes exactly one lock of the user. If there are no remaining locks the user is removed from the cache after a
   * maximum of 5 minutes.
   *
   * @param user the user to remove a lock for.
   * @throws NullPointerException if the given user is null.
   */
  void unlock(@NonNull PermissionUser user);

  /**
   * Removes exactly one lock of the group. If there are no remaining locks the group is removed from the cache after a
   * maximum of 5 minutes.
   *
   * @param group the group to remove a lock for.
   * @throws NullPointerException if the given group is null.
   */
  void unlock(@NonNull PermissionGroup group);

  /**
   * Removes all locks that the given user has. The user is removed from the cache after a maximum timeout of 5
   * minutes.
   *
   * @param user the user to unlock completely.
   * @throws NullPointerException if the given user is null.
   */
  void unlockFully(@NonNull PermissionUser user);

  /**
   * Removes all locks that the given group has. The group is removed from the cache after a maximum timeout of 5
   * minutes.
   *
   * @param group the group to unlock completely.
   * @throws NullPointerException if the given group is null.
   */
  void unlockFully(@NonNull PermissionGroup group);
}
