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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * This management extends the default implementation of the permission management by implementing all methods needed
 * for caching of both permission users and groups. The caches are backed by Caffeine and expire after 5 minutes without
 * any access or locks on the specific permissible.
 *
 * @see eu.cloudnetservice.cloudnet.driver.permission.CachedPermissionManagement
 * @since 4.0
 */
public abstract class DefaultCachedPermissionManagement extends DefaultPermissionManagement
  implements CachedPermissionManagement {

  protected final Map<UUID, AtomicInteger> permissionUserLocks = new ConcurrentHashMap<>();
  protected final Map<String, AtomicInteger> permissionGroupLocks = new ConcurrentHashMap<>();

  // holds all cached permission users and tries to unload them after 5 minutes of inactivity
  // will be prevented if a lock is known for the given player object, for example when connected
  protected final Cache<UUID, PermissionUser> permissionUserCache = Caffeine.newBuilder()
    .expireAfterAccess(5, TimeUnit.MINUTES)
    .removalListener((key, value, cause) -> {
      if (key != null && value != null) {
        this.handleUserRemove((UUID) key, (PermissionUser) value, cause);
      }
    })
    .build();
  // holds all cached permission groups, removes will get blocked if a lock for a group was obtained
  protected final Cache<String, PermissionGroup> permissionGroupCache = Caffeine.newBuilder()
    .removalListener((key, value, cause) -> {
      if (key != null && value != null) {
        this.handleGroupRemove((String) key, (PermissionGroup) value, cause);
      }
    })
    .build();

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Map<UUID, PermissionUser> cachedPermissionUsers() {
    return this.permissionUserCache.asMap();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Map<String, PermissionGroup> cachedPermissionGroups() {
    return this.permissionGroupCache.asMap();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable PermissionUser cachedUser(@NonNull UUID uniqueId) {
    return this.permissionUserCache.getIfPresent(uniqueId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable PermissionGroup cachedGroup(@NonNull String name) {
    return this.permissionGroupCache.getIfPresent(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void acquireLock(@NonNull PermissionUser user) {
    this.permissionUserLocks.computeIfAbsent(user.uniqueId(), uuid -> new AtomicInteger()).incrementAndGet();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void acquireLock(@NonNull PermissionGroup group) {
    this.permissionGroupLocks.computeIfAbsent(group.name(), name -> new AtomicInteger()).incrementAndGet();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean locked(@NonNull PermissionUser user) {
    var lockCount = this.permissionUserLocks.get(user.uniqueId());
    return lockCount != null && lockCount.get() > 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean locked(@NonNull PermissionGroup group) {
    var lockCount = this.permissionGroupLocks.get(group.name());
    return lockCount != null && lockCount.get() > 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unlock(@NonNull PermissionUser user) {
    var lockCount = this.permissionUserLocks.get(user.uniqueId());
    if (lockCount != null) {
      // only decrement the count if we are above 0
      lockCount.updateAndGet(count -> Math.max(0, count - 1));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unlock(@NonNull PermissionGroup group) {
    var lockCount = this.permissionGroupLocks.get(group.name());
    if (lockCount != null) {
      // only decrement the count if we are above 0
      lockCount.updateAndGet(count -> Math.max(0, count - 1));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unlockFully(@NonNull PermissionUser user) {
    this.permissionUserLocks.remove(user.uniqueId());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unlockFully(@NonNull PermissionGroup group) {
    this.permissionGroupLocks.remove(group.name());
  }

  /**
   * Handles the removal of the permission user in the cache. If the user still is locked and wasn't removed because he
   * is replaced the user is added back to the cache.
   *
   * @param key   the unique id of the removed user.
   * @param user  the removed user.
   * @param cause the reason for the removal out of the cache.
   * @throws NullPointerException if the given key, user or cause is null.
   */
  protected void handleUserRemove(@NonNull UUID key, @NonNull PermissionUser user, @NonNull RemovalCause cause) {
    if (cause.wasEvicted() && this.locked(user)) {
      this.permissionUserCache.put(key, user);
    }
  }

  /**
   * Handles the removal of the permission group in the cache. If the group still is locked and wasn't removed because
   * it is replaced the group is added back to the cache.
   *
   * @param key   the unique id of the removed group.
   * @param group the removed group.
   * @param cause the reason for the removal out of the cache.
   * @throws NullPointerException if the given key, group or cause is null.
   */
  protected void handleGroupRemove(@NonNull String key, @NonNull PermissionGroup group, @NonNull RemovalCause cause) {
    if (cause.wasEvicted() && this.locked(group)) {
      this.permissionGroupCache.put(key, group);
    }
  }
}
