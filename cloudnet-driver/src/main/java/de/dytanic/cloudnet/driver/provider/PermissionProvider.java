package de.dytanic.cloudnet.driver.provider;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PermissionProvider {

    void addUser(IPermissionUser permissionUser);

    void updateUser(IPermissionUser permissionUser);

    void deleteUser(String name);

    void deleteUser(IPermissionUser permissionUser);

    boolean containsUser(UUID uniqueId);

    boolean containsUser(String name);

    IPermissionUser getUser(UUID uniqueId);

    List<IPermissionUser> getUsers(String name);

    Collection<IPermissionUser> getUsers();

    void setUsers(Collection<? extends IPermissionUser> users);

    Collection<IPermissionUser> getUsersByGroup(String group);

    void addGroup(IPermissionGroup permissionGroup);

    void updateGroup(IPermissionGroup permissionGroup);

    void deleteGroup(String name);

    void deleteGroup(IPermissionGroup permissionGroup);

    boolean containsGroup(String group);

    IPermissionGroup getGroup(String name);

    Collection<IPermissionGroup> getGroups();

    void setGroups(Collection<? extends IPermissionGroup> groups);

    ITask<Void> addUserAsync(IPermissionUser permissionUser);

    ITask<Void> updateUserAsync(IPermissionUser permissionUser);

    ITask<Void> deleteUserAsync(String name);

    ITask<Void> deleteUserAsync(IPermissionUser permissionUser);

    ITask<Boolean> containsUserAsync(UUID uniqueId);

    ITask<Boolean> containsUserAsync(String name);

    ITask<IPermissionUser> getUserAsync(UUID uniqueId);

    ITask<List<IPermissionUser>> getUsersAsync(String name);

    ITask<Collection<IPermissionUser>> getUsersAsync();

    ITask<Void> setUsersAsync(Collection<? extends IPermissionUser> users);

    ITask<Collection<IPermissionUser>> getUsersByGroupAsync(String group);

    ITask<Void> addGroupAsync(IPermissionGroup permissionGroup);

    ITask<Void> updateGroupAsync(IPermissionGroup permissionGroup);

    ITask<Void> deleteGroupAsync(String name);

    ITask<Void> deleteGroupAsync(IPermissionGroup permissionGroup);

    ITask<Boolean> containsGroupAsync(String name);

    ITask<IPermissionGroup> getGroupAsync(String name);

    ITask<Collection<IPermissionGroup>> getGroupsAsync();

    ITask<Void> setGroupsAsync(Collection<? extends IPermissionGroup> groups);

}
