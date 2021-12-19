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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public abstract class DefaultCachedPermissionManagement extends DefaultPermissionManagement
  implements CachedPermissionManagement {

  protected final Map<UUID, AtomicInteger> permissionUserLocks = new ConcurrentHashMap<>();
  protected final Map<String, AtomicInteger> permissionGroupLocks = new ConcurrentHashMap<>();

  protected final Cache<UUID, PermissionUser> permissionUserCache = CacheBuilder.newBuilder()
    .expireAfterAccess(5, TimeUnit.MINUTES)
    .concurrencyLevel(4)
    .removalListener(notification -> this.handleUserRemove(
      (UUID) notification.getKey(),
      (PermissionUser) notification.getValue(),
      notification.getCause()))
    .build();

  protected final Cache<String, PermissionGroup> permissionGroupCache = CacheBuilder.newBuilder()
    .concurrencyLevel(4)
    .removalListener(notification -> this.handleGroupRemove(
      (String) notification.getKey(),
      (PermissionGroup) notification.getValue(),
      notification.getCause()))
    .build();

  @Override
  public @NonNull Map<UUID, PermissionUser> cachedPermissionUsers() {
    return this.permissionUserCache.asMap();
  }

  @Override
  public @NonNull Map<String, PermissionGroup> cachedPermissionGroups() {
    return this.permissionGroupCache.asMap();
  }

  @Override
  public @Nullable PermissionUser cachedUser(@NonNull UUID uniqueId) {
    return this.permissionUserCache.getIfPresent(uniqueId);
  }

  @Override
  public @Nullable PermissionGroup cachedGroup(@NonNull String name) {
    return this.permissionGroupCache.getIfPresent(name);
  }

  @Override
  public void acquireLock(@NonNull PermissionUser user) {
    this.permissionUserLocks.computeIfAbsent(user.uniqueId(), uuid -> new AtomicInteger()).incrementAndGet();
  }

  @Override
  public void acquireLock(@NonNull PermissionGroup group) {
    this.permissionGroupLocks.computeIfAbsent(group.name(), name -> new AtomicInteger()).incrementAndGet();
  }

  @Override
  public boolean locked(@NonNull PermissionUser user) {
    var lockCount = this.permissionUserLocks.get(user.uniqueId());
    return lockCount != null && lockCount.get() > 0;
  }

  @Override
  public boolean locked(@NonNull PermissionGroup group) {
    var lockCount = this.permissionGroupLocks.get(group.name());
    return lockCount != null && lockCount.get() > 0;
  }

  @Override
  public void unlock(@NonNull PermissionUser user) {
    var lockCount = this.permissionUserLocks.get(user.uniqueId());
    if (lockCount != null) {
      lockCount.decrementAndGet();
    }
  }

  @Override
  public void unlock(@NonNull PermissionGroup group) {
    var lockCount = this.permissionGroupLocks.get(group.name());
    if (lockCount != null) {
      lockCount.decrementAndGet();
    }
  }

  @Override
  public void unlockFully(@NonNull PermissionUser user) {
    this.permissionUserLocks.remove(user.uniqueId());
  }

  @Override
  public void unlockFully(@NonNull PermissionGroup group) {
    this.permissionGroupLocks.remove(group.name());
  }

  protected void handleUserRemove(@NonNull UUID key, @NonNull PermissionUser user, @NonNull RemovalCause cause) {
    if (cause != RemovalCause.REPLACED && this.locked(user)) {
      this.permissionUserCache.put(key, user);
    }
  }

  protected void handleGroupRemove(@NonNull String key, @NonNull PermissionGroup group, @NonNull RemovalCause cause) {
    if (cause != RemovalCause.REPLACED && this.locked(group)) {
      this.permissionGroupCache.put(key, group);
    }
  }
}
