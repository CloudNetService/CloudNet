package de.dytanic.cloudnet.permission;
/*
 * Created by derrop on 28.10.2019
 */

import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;

import java.util.Collection;

public interface ClusterSynchronizedPermissionManagement extends IPermissionManagement {

    default IPermissionUser addUser(IPermissionUser permissionUser) {

    }

    default void updateUser(IPermissionUser permissionUser) {

    }

    default void deleteUser(String name) {

    }

    default void deleteUser(IPermissionUser permissionUser) {

    }

    default void setUsers(Collection<? extends IPermissionUser> users) {

    }

    default IPermissionGroup addGroup(IPermissionGroup permissionGroup) {

    }

    default void updateGroup(IPermissionGroup permissionGroup) {

    }

    default void deleteGroup(String group) {

    }

    default void deleteGroup(IPermissionGroup group) {

    }

    default void setGroups(Collection<? extends IPermissionGroup> groups) {

    }

    boolean isUpdatingUsersToCluster();

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
