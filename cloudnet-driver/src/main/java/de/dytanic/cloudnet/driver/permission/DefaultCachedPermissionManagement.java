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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class DefaultCachedPermissionManagement extends DefaultPermissionManagement implements
  CachedPermissionManagement {

  protected final Map<UUID, AtomicInteger> permissionUserLocks = new ConcurrentHashMap<>();
  protected final Cache<UUID, IPermissionUser> permissionUserCache = CacheBuilder.newBuilder()
    .expireAfterAccess(5, TimeUnit.MINUTES)
    .concurrencyLevel(4)
    .removalListener(notification -> this
      .handleUserRemove((UUID) notification.getKey(), (IPermissionUser) notification.getValue(),
        notification.getCause()))
    .build();
  protected final Map<String, AtomicInteger> permissionGroupLocks = new ConcurrentHashMap<>();
  protected final Cache<String, IPermissionGroup> permissionGroupCache = CacheBuilder.newBuilder()
    .concurrencyLevel(4)
    .removalListener(notification -> this
      .handleGroupRemove((String) notification.getKey(), (IPermissionGroup) notification.getValue(),
        notification.getCause()))
    .build();

  @Override
  public Map<UUID, IPermissionUser> getCachedPermissionUsers() {
    return this.permissionUserCache.asMap();
  }

  @Override
  public Map<String, IPermissionGroup> getCachedPermissionGroups() {
    return this.permissionGroupCache.asMap();
  }

  @Override
  public @Nullable IPermissionUser getCachedUser(UUID uniqueId) {
    return this.permissionUserCache.getIfPresent(uniqueId);
  }

  @Override
  public @Nullable IPermissionGroup getCachedGroup(String name) {
    return this.permissionGroupCache.getIfPresent(name);
  }

  @Override
  public void acquireLock(IPermissionUser user) {
    this.permissionUserLocks.computeIfAbsent(user.getUniqueId(), uuid -> new AtomicInteger()).incrementAndGet();
  }

  @Override
  public void acquireLock(IPermissionGroup group) {
    this.permissionGroupLocks.computeIfAbsent(group.getName(), name -> new AtomicInteger()).incrementAndGet();
  }

  @Override
  public boolean isLocked(IPermissionUser user) {
    AtomicInteger lockCount = this.permissionUserLocks.get(user.getUniqueId());
    return lockCount != null && lockCount.get() > 0;
  }

  @Override
  public boolean isLocked(IPermissionGroup group) {
    AtomicInteger lockCount = this.permissionGroupLocks.get(group.getName());
    return lockCount != null && lockCount.get() > 0;
  }

  @Override
  public void unlock(IPermissionUser user) {
    AtomicInteger lockCount = this.permissionUserLocks.get(user.getUniqueId());
    if (lockCount != null) {
      lockCount.decrementAndGet();
    }
  }

  @Override
  public void unlock(IPermissionGroup group) {
    AtomicInteger lockCount = this.permissionGroupLocks.get(group.getName());
    if (lockCount != null) {
      lockCount.decrementAndGet();
    }
  }

  @Override
  public void unlockFully(IPermissionUser user) {
    this.permissionUserLocks.remove(user.getUniqueId());
  }

  @Override
  public void unlockFully(IPermissionGroup group) {
    this.permissionGroupLocks.remove(group.getName());
  }

  protected void handleUserRemove(@NotNull UUID key, @NotNull IPermissionUser user, @NotNull RemovalCause cause) {
    if (cause != RemovalCause.REPLACED && this.isLocked(user)) {
      this.permissionUserCache.put(key, user);
    }
  }

  protected void handleGroupRemove(@NotNull String key, @NotNull IPermissionGroup group, @NotNull RemovalCause cause) {
    if (cause != RemovalCause.REPLACED && this.isLocked(group)) {
      this.permissionGroupCache.put(key, group);
    }
  }
}
