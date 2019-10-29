package de.dytanic.cloudnet.driver.permission;

import de.dytanic.cloudnet.common.Validate;

import java.util.Collection;
import java.util.Collections;

public interface ClusterSynchronizedPermissionManagement extends IPermissionManagement {

    default IPermissionUser addUser(IPermissionUser permissionUser) {
        Validate.checkNotNull(permissionUser);
        if (getPermissionManagementHandler() != null) {
            getPermissionManagementHandler().handleAddUser(this, permissionUser);
        }
        return this.addUserWithoutClusterSync(permissionUser);
    }

    default void updateUser(IPermissionUser permissionUser) {
        Validate.checkNotNull(permissionUser);
        if (getPermissionManagementHandler() != null) {
            getPermissionManagementHandler().handleUpdateUser(this, permissionUser);
        }
        this.updateUserWithoutClusterSync(permissionUser);
    }

    default void deleteUser(String name) {
        Validate.checkNotNull(name);
        for (IPermissionUser permissionUser : this.getUser(name)) {
            this.deleteUser(permissionUser);
        }
    }

    default void deleteUser(IPermissionUser permissionUser) {
        Validate.checkNotNull(permissionUser);
        if (getPermissionManagementHandler() != null) {
            getPermissionManagementHandler().handleDeleteUser(this, permissionUser);
        }
        this.deleteUserWithoutClusterSync(permissionUser);
    }

    default void setUsers(Collection<? extends IPermissionUser> users) {
        if (users == null) {
            users = Collections.emptyList();
        }
        if (getPermissionManagementHandler() != null) {
            getPermissionManagementHandler().handleSetUsers(this, users);
        }

        this.setUsersWithoutClusterSync(users);
    }

    default IPermissionGroup addGroup(IPermissionGroup permissionGroup) {
        Validate.checkNotNull(permissionGroup);
        if (getPermissionManagementHandler() != null) {
            getPermissionManagementHandler().handleAddGroup(this, permissionGroup);
        }
        addGroupWithoutClusterSync(permissionGroup);
        return permissionGroup;
    }

    default void updateGroup(IPermissionGroup permissionGroup) {
        Validate.checkNotNull(permissionGroup);
        if (getPermissionManagementHandler() != null) {
            getPermissionManagementHandler().handleUpdateGroup(this, permissionGroup);
        }
        this.updateGroupWithoutClusterSync(permissionGroup);
    }

    default void deleteGroup(String group) {
        this.deleteGroup(this.getGroup(group));
    }

    default void deleteGroup(IPermissionGroup group) {
        if (getPermissionManagementHandler() != null) {
            getPermissionManagementHandler().handleDeleteGroup(this, group);
        }
        this.deleteGroupWithoutClusterSync(group);
    }

    default void setGroups(Collection<? extends IPermissionGroup> groups) {
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
