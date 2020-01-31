package de.dytanic.cloudnet.driver.permission;

import de.dytanic.cloudnet.common.collection.Iterables;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public interface IPermissionUser extends IPermissible {

    @NotNull
    UUID getUniqueId();

    Collection<PermissionUserGroupInfo> getGroups();

    @Nullable
    String getHashedPassword();

    void changePassword(String password);

    boolean checkPassword(String password);


    default IPermissionUser addGroup(@NotNull String group) {
        return addGroup(group, 0L);
    }

    default IPermissionUser addGroup(@NotNull String group, long time, TimeUnit timeUnit) {
        return addGroup(group, (System.currentTimeMillis() + timeUnit.toMillis(time)));
    }

    default IPermissionUser addGroup(@NotNull String group, long timeOutMillis) {
        PermissionUserGroupInfo groupInfo = Iterables.first(getGroups(), permissionUserGroupInfo -> permissionUserGroupInfo.getGroup().equalsIgnoreCase(group));

        if (groupInfo != null) {
            removeGroup(groupInfo.getGroup());
        }

        groupInfo = new PermissionUserGroupInfo(group, timeOutMillis);

        getGroups().add(groupInfo);
        return this;
    }

    default IPermissionUser removeGroup(@NotNull String group) {
        Collection<PermissionUserGroupInfo> groupInfo = Iterables.filter(getGroups(), permissionUserGroupInfo -> permissionUserGroupInfo.getGroup().equalsIgnoreCase(group));

        getGroups().removeAll(groupInfo);

        return this;
    }

    default boolean inGroup(@NotNull String group) {
        return Iterables.first(getGroups(), permissionUserGroupInfo -> permissionUserGroupInfo.getGroup().equalsIgnoreCase(group)) != null;
    }
}