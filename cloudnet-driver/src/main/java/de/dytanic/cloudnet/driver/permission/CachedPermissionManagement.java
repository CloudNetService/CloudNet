package de.dytanic.cloudnet.driver.permission;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public interface CachedPermissionManagement extends IPermissionManagement {

    Map<UUID, IPermissionUser> getCachedPermissionUsers();

    Map<String, IPermissionGroup> getCachedPermissionGroups();

    @Nullable IPermissionUser getCachedUser(UUID uniqueId);

    @Nullable IPermissionGroup getCachedGroup(String name);

    void acquireLock(IPermissionUser user);

    void acquireLock(IPermissionGroup group);

    boolean isLocked(IPermissionUser user);

    boolean isLocked(IPermissionGroup group);

    void unlock(IPermissionUser user);

    void unlock(IPermissionGroup group);

    void unlockFully(IPermissionUser user);

    void unlockFully(IPermissionGroup group);
}
