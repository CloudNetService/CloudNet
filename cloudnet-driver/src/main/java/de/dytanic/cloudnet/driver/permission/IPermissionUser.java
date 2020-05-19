package de.dytanic.cloudnet.driver.permission;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public interface IPermissionUser extends IPermissible {

    @NotNull
    UUID getUniqueId();

    Collection<PermissionUserGroupInfo> getGroups();

    @Nullable
    String getHashedPassword();

    void changePassword(String password);

    boolean checkPassword(String password);


    default IPermissionUser addGroup(@NotNull String group) {
        return this.addGroup(group, 0L);
    }

    default IPermissionUser addGroup(@NotNull String group, long time, TimeUnit timeUnit) {
        return this.addGroup(group, (System.currentTimeMillis() + timeUnit.toMillis(time)));
    }

    default IPermissionUser addGroup(@NotNull String group, long timeOutMillis) {
        PermissionUserGroupInfo groupInfo = this.getGroups().stream()
                .filter(permissionUserGroupInfo -> permissionUserGroupInfo.getGroup().equalsIgnoreCase(group))
                .findFirst().orElse(null);

        if (groupInfo != null) {
            this.removeGroup(groupInfo.getGroup());
        }

        groupInfo = new PermissionUserGroupInfo(group, timeOutMillis);

        this.getGroups().add(groupInfo);
        return this;
    }

    default IPermissionUser removeGroup(@NotNull String group) {
        Collection<PermissionUserGroupInfo> groupInfo = this.getGroups().stream()
                .filter(permissionUserGroupInfo -> permissionUserGroupInfo.getGroup().equalsIgnoreCase(group)).collect(Collectors.toList());

        this.getGroups().removeAll(groupInfo);

        return this;
    }

    default boolean inGroup(@NotNull String group) {
        return this.getGroups().stream().anyMatch(permissionUserGroupInfo -> permissionUserGroupInfo.getGroup().equalsIgnoreCase(group));
    }
}