package de.dytanic.cloudnet.driver.provider;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PermissionProvider {

    void addUser(@NotNull IPermissionUser permissionUser);

    void updateUser(@NotNull IPermissionUser permissionUser);

    void deleteUser(@NotNull String name);

    void deleteUser(@NotNull IPermissionUser permissionUser);

    boolean containsUser(@NotNull UUID uniqueId);

    boolean containsUser(@NotNull String name);

    @Nullable
    IPermissionUser getUser(@NotNull UUID uniqueId);

    List<IPermissionUser> getUsers(@NotNull String name);

    Collection<IPermissionUser> getUsers();

    void setUsers(@NotNull Collection<? extends IPermissionUser> users);

    Collection<IPermissionUser> getUsersByGroup(@NotNull String group);

    void addGroup(@NotNull IPermissionGroup permissionGroup);

    void updateGroup(@NotNull IPermissionGroup permissionGroup);

    void deleteGroup(@NotNull String name);

    void deleteGroup(@NotNull IPermissionGroup permissionGroup);

    boolean containsGroup(@NotNull String group);

    @Nullable
    IPermissionGroup getGroup(@NotNull String name);

    Collection<IPermissionGroup> getGroups();

    void setGroups(@NotNull Collection<? extends IPermissionGroup> groups);

    @NotNull
    ITask<Void> addUserAsync(@NotNull IPermissionUser permissionUser);

    @NotNull
    ITask<Void> updateUserAsync(@NotNull IPermissionUser permissionUser);

    @NotNull
    ITask<Void> deleteUserAsync(@NotNull String name);

    @NotNull
    ITask<Void> deleteUserAsync(@NotNull IPermissionUser permissionUser);

    @NotNull
    ITask<Boolean> containsUserAsync(@NotNull UUID uniqueId);

    @NotNull
    ITask<Boolean> containsUserAsync(@NotNull String name);

    @NotNull
    ITask<IPermissionUser> getUserAsync(@NotNull UUID uniqueId);

    @NotNull
    ITask<List<IPermissionUser>> getUsersAsync(@NotNull String name);

    @NotNull
    ITask<Collection<IPermissionUser>> getUsersAsync();

    @NotNull
    ITask<Void> setUsersAsync(@NotNull Collection<? extends IPermissionUser> users);

    @NotNull
    ITask<Collection<IPermissionUser>> getUsersByGroupAsync(@NotNull String group);

    @NotNull
    ITask<Void> addGroupAsync(@NotNull IPermissionGroup permissionGroup);

    @NotNull
    ITask<Void> updateGroupAsync(@NotNull IPermissionGroup permissionGroup);

    @NotNull
    ITask<Void> deleteGroupAsync(@NotNull String name);

    @NotNull
    ITask<Void> deleteGroupAsync(@NotNull IPermissionGroup permissionGroup);

    @NotNull
    ITask<Boolean> containsGroupAsync(@NotNull String name);

    @NotNull
    ITask<IPermissionGroup> getGroupAsync(@NotNull String name);

    @NotNull
    ITask<Collection<IPermissionGroup>> getGroupsAsync();

    @NotNull
    ITask<Void> setGroupsAsync(@NotNull Collection<? extends IPermissionGroup> groups);

}
