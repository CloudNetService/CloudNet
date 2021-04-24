package de.dytanic.cloudnet.driver.permission;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class DefaultCachedPermissionManagement extends DefaultPermissionManagement implements CachedPermissionManagement {

    protected final Cache<UUID, IPermissionUser> permissionUserCache = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .concurrencyLevel(4)
            .removalListener(notification -> this.handleUserRemove((UUID) notification.getKey(), (IPermissionUser) notification.getValue()))
            .build();
    protected final Cache<String, IPermissionGroup> permissionGroupCache = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .removalListener(notification -> this.handleGroupRemove((String) notification.getKey(), (IPermissionGroup) notification.getValue()))
            .build();

    protected final Map<UUID, AtomicInteger> permissionUserLocks = new ConcurrentHashMap<>();
    protected final Map<String, AtomicInteger> permissionGroupLocks = new ConcurrentHashMap<>();

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

    protected void handleUserRemove(@NotNull UUID key, @NotNull IPermissionUser user) {
        if (this.isLocked(user)) {
            this.permissionUserCache.put(key, user);
        }
    }

    protected void handleGroupRemove(@NotNull String key, @NotNull IPermissionGroup group) {
        if (this.isLocked(group)) {
            this.permissionGroupCache.put(key, group);
        }
    }
}
