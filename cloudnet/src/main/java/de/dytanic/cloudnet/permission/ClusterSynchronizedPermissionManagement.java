package de.dytanic.cloudnet.permission;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public interface ClusterSynchronizedPermissionManagement extends NodePermissionManagement {

    default IPermissionUser addUser(@NotNull IPermissionUser permissionUser) {
        Preconditions.checkNotNull(permissionUser);
        if (getPermissionManagementHandler() != null) {
            getPermissionManagementHandler().handleAddUser(this, permissionUser);
        }
        return this.addUserWithoutClusterSync(permissionUser);
    }

    default void updateUser(@NotNull IPermissionUser permissionUser) {
        Preconditions.checkNotNull(permissionUser);
        if (getPermissionManagementHandler() != null) {
            getPermissionManagementHandler().handleUpdateUser(this, permissionUser);
        }
        this.updateUserWithoutClusterSync(permissionUser);
    }

    default void deleteUser(@NotNull String name) {
        Preconditions.checkNotNull(name);
        for (IPermissionUser permissionUser : this.getUsers(name)) {
            this.deleteUser(permissionUser);
        }
    }

    default void deleteUser(@NotNull IPermissionUser permissionUser) {
        Preconditions.checkNotNull(permissionUser);
        if (getPermissionManagementHandler() != null) {
            getPermissionManagementHandler().handleDeleteUser(this, permissionUser);
        }
        this.deleteUserWithoutClusterSync(permissionUser);
    }

    default void setUsers(@Nullable Collection<? extends IPermissionUser> users) {
        if (users == null) {
            users = Collections.emptyList();
        }
        if (getPermissionManagementHandler() != null) {
            getPermissionManagementHandler().handleSetUsers(this, users);
        }

        this.setUsersWithoutClusterSync(users);
    }

    default IPermissionGroup addGroup(@NotNull IPermissionGroup permissionGroup) {
        Preconditions.checkNotNull(permissionGroup);
        if (getPermissionManagementHandler() != null) {
            getPermissionManagementHandler().handleAddGroup(this, permissionGroup);
        }
        addGroupWithoutClusterSync(permissionGroup);
        return permissionGroup;
    }

    default void updateGroup(@NotNull IPermissionGroup permissionGroup) {
        Preconditions.checkNotNull(permissionGroup);
        if (getPermissionManagementHandler() != null) {
            getPermissionManagementHandler().handleUpdateGroup(this, permissionGroup);
        }
        this.updateGroupWithoutClusterSync(permissionGroup);
    }

    default void deleteGroup(@NotNull String group) {
        if (this.containsGroup(group)) {
            this.deleteGroup(this.getGroup(group));
        }
    }

    default void deleteGroup(@NotNull IPermissionGroup group) {
        if (getPermissionManagementHandler() != null) {
            getPermissionManagementHandler().handleDeleteGroup(this, group);
        }
        this.deleteGroupWithoutClusterSync(group);
    }

    default void setGroups(@NotNull Collection<? extends IPermissionGroup> groups) {
        if (groups == null) {
            groups = Collections.emptyList();
        }
        if (getPermissionManagementHandler() != null) {
            getPermissionManagementHandler().handleSetGroups(this, groups);
        }
        setGroupsWithoutClusterSync(groups);
    }

    IPermissionUser addUserWithoutClusterSync(IPermissionUser permissionUser);

    void updateUserWithoutClusterSync(IPermissionUser permissionUser);

    void deleteUserWithoutClusterSync(String name);

    void deleteUserWithoutClusterSync(IPermissionUser permissionUser);

    void setUsersWithoutClusterSync(Collection<? extends IPermissionUser> users);

    IPermissionGroup addGroupWithoutClusterSync(IPermissionGroup permissionGroup);

    void updateGroupWithoutClusterSync(IPermissionGroup permissionGroup);

    void deleteGroupWithoutClusterSync(String group);

    void deleteGroupWithoutClusterSync(IPermissionGroup group);

    void setGroupsWithoutClusterSync(Collection<? extends IPermissionGroup> groups);

}
