package de.dytanic.cloudnet.driver.permission;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public interface DefaultSynchronizedPermissionManagement extends IPermissionManagement {

    @Override
    default IPermissionUser getFirstUser(String name) {
        List<IPermissionUser> users = this.getUsers(name);
        return users.isEmpty() ? null : users.get(0);
    }

    @Override
    default IPermissionGroup getDefaultPermissionGroup() {
        return this.getDefaultPermissionGroupAsync().get(5, TimeUnit.SECONDS, null);
    }

    @Override
    default IPermissionUser addUser(@NotNull String name, @NotNull String password, int potency) {
        return this.addUserAsync(name, password, potency).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    default IPermissionGroup addGroup(@NotNull String role, int potency) {
        return this.addGroupAsync(role, potency).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    default IPermissionUser addUser(@NotNull IPermissionUser permissionUser) {
        return this.addUserAsync(permissionUser).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    default void updateUser(@NotNull IPermissionUser permissionUser) {
        this.updateUserAsync(permissionUser).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    default boolean deleteUser(@NotNull String name) {
        return this.deleteUserAsync(name).get(5, TimeUnit.SECONDS, false);
    }

    @Override
    default boolean deleteUser(@NotNull IPermissionUser permissionUser) {
        return this.deleteUserAsync(permissionUser).get(5, TimeUnit.SECONDS, false);
    }

    @Override
    default boolean containsUser(@NotNull UUID uniqueId) {
        return this.containsUserAsync(uniqueId).get(5, TimeUnit.SECONDS, false);
    }

    @Override
    default boolean containsUser(@NotNull String name) {
        return this.containsUserAsync(name).get(5, TimeUnit.SECONDS, false);
    }

    @Override
    default @Nullable IPermissionUser getUser(@NotNull UUID uniqueId) {
        return this.getUserAsync(uniqueId).get(5, TimeUnit.SECONDS, null);
    }

    @NotNull
    @Override
    default List<IPermissionUser> getUsers(@NotNull String name) {
        return this.getUsersAsync(name).get(5, TimeUnit.SECONDS, Collections.emptyList());
    }

    @Override
    default @NotNull Collection<IPermissionUser> getUsers() {
        return this.getUsersAsync().get(5, TimeUnit.SECONDS, Collections.emptyList());
    }

    @Override
    default void setUsers(@NotNull Collection<? extends IPermissionUser> users) {
        this.setUsersAsync(users).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    default Collection<IPermissionUser> getUsersByGroup(@NotNull String group) {
        return this.getUsersByGroupAsync(group).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    default IPermissionGroup addGroup(@NotNull IPermissionGroup permissionGroup) {
        return this.addGroupAsync(permissionGroup).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    default void updateGroup(@NotNull IPermissionGroup permissionGroup) {
        this.updateGroupAsync(permissionGroup).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    default void deleteGroup(@NotNull String name) {
        this.deleteGroupAsync(name).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    default void deleteGroup(@NotNull IPermissionGroup permissionGroup) {
        this.deleteGroupAsync(permissionGroup).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    default boolean containsGroup(@NotNull String group) {
        return this.containsGroupAsync(group).get(5, TimeUnit.SECONDS, false);
    }

    @Override
    default @Nullable IPermissionGroup getGroup(@NotNull String name) {
        return this.getGroupAsync(name).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    default void setGroups(Collection<? extends IPermissionGroup> groups) {
        this.setGroupsAsync(groups).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    default IPermissionGroup modifyGroup(@NotNull String name, @NotNull Consumer<IPermissionGroup> modifier) {
        return this.modifyGroupAsync(name, modifier).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    default List<IPermissionUser> modifyUsers(@NotNull String name, @NotNull Consumer<IPermissionUser> modifier) {
        return this.modifyUsersAsync(name, modifier).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    default IPermissionUser modifyUser(@NotNull UUID uniqueId, @NotNull Consumer<IPermissionUser> modifier) {
        return this.modifyUserAsync(uniqueId, modifier).get(5, TimeUnit.SECONDS, null);
    }
}
