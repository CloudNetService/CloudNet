package de.dytanic.cloudnet.driver.provider;

import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import de.dytanic.cloudnet.common.concurrent.NullCompletableTask;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class DefaultPermissionProvider implements PermissionProvider {

    private Supplier<IPermissionManagement> permissionManagementSupplier;

    public DefaultPermissionProvider(Supplier<IPermissionManagement> permissionManagementSupplier) {
        this.permissionManagementSupplier = permissionManagementSupplier;
    }

    @Override
    public void addUser(@NotNull IPermissionUser permissionUser) {
        this.permissionManagementSupplier.get().addUser(permissionUser);
    }

    @Override
    public void updateUser(@NotNull IPermissionUser permissionUser) {
        this.permissionManagementSupplier.get().updateUser(permissionUser);
    }

    @Override
    public void deleteUser(@NotNull String name) {
        this.permissionManagementSupplier.get().deleteUser(name);
    }

    @Override
    public void deleteUser(@NotNull IPermissionUser permissionUser) {
        this.permissionManagementSupplier.get().deleteUser(permissionUser);
    }

    @Override
    public boolean containsUser(@NotNull UUID uniqueId) {
        return this.permissionManagementSupplier.get().containsUser(uniqueId);
    }

    @Override
    public boolean containsUser(@NotNull String name) {
        return this.permissionManagementSupplier.get().containsUser(name);
    }

    @Override
    public @Nullable IPermissionUser getUser(@NotNull UUID uniqueId) {
        return this.permissionManagementSupplier.get().getUser(uniqueId);
    }

    @Override
    public List<IPermissionUser> getUsers(@NotNull String name) {
        return this.permissionManagementSupplier.get().getUsers(name);
    }

    @Override
    public Collection<IPermissionUser> getUsers() {
        return this.permissionManagementSupplier.get().getUsers();
    }

    @Override
    public void setUsers(@Nullable Collection<? extends IPermissionUser> users) {
        this.permissionManagementSupplier.get().setUsers(users);
    }

    @Override
    public Collection<IPermissionUser> getUsersByGroup(@NotNull String group) {
        return this.permissionManagementSupplier.get().getUsersByGroup(group);
    }

    @Override
    public void addGroup(@NotNull IPermissionGroup permissionGroup) {
        this.permissionManagementSupplier.get().addGroup(permissionGroup);
    }

    @Override
    public void updateGroup(@NotNull IPermissionGroup permissionGroup) {
        this.permissionManagementSupplier.get().updateGroup(permissionGroup);
    }

    @Override
    public void deleteGroup(@NotNull String name) {
        this.permissionManagementSupplier.get().deleteGroup(name);
    }

    @Override
    public void deleteGroup(@NotNull IPermissionGroup permissionGroup) {
        this.permissionManagementSupplier.get().deleteGroup(permissionGroup);
    }

    @Override
    public boolean containsGroup(@NotNull String group) {
        return this.permissionManagementSupplier.get().containsGroup(group);
    }

    @Override
    public @Nullable IPermissionGroup getGroup(@NotNull String name) {
        return this.permissionManagementSupplier.get().getGroup(name);
    }

    @Override
    public Collection<IPermissionGroup> getGroups() {
        return this.permissionManagementSupplier.get().getGroups();
    }

    @Override
    public void setGroups(@NotNull Collection<? extends IPermissionGroup> groups) {
        this.permissionManagementSupplier.get().setGroups(groups);
    }

    @Override
    public @NotNull ITask<Void> addUserAsync(@NotNull IPermissionUser permissionUser) {
        return ((ListenableTask<IPermissionUser>) this.permissionManagementSupplier.get().addUserAsync(permissionUser)).map(o -> null);
    }

    @Override
    public @NotNull ITask<Void> updateUserAsync(@NotNull IPermissionUser permissionUser) {
        return this.permissionManagementSupplier.get().updateUserAsync(permissionUser);
    }

    @Override
    public @NotNull ITask<Void> deleteUserAsync(@NotNull String name) {
        CompletableTask<Void> task = new NullCompletableTask<>();
        this.permissionManagementSupplier.get().deleteUserAsync(name)
                .onComplete(success -> task.call())
                .onCancelled(booleanITask -> task.call())
                .onFailure(throwable -> task.call());
        return task;
    }

    @Override
    public @NotNull ITask<Void> deleteUserAsync(@NotNull IPermissionUser permissionUser) {
        CompletableTask<Void> task = new NullCompletableTask<>();
        this.permissionManagementSupplier.get().deleteUserAsync(permissionUser)
                .onComplete(success -> task.call())
                .onCancelled(booleanITask -> task.call())
                .onFailure(throwable -> task.call());
        return task;
    }

    @Override
    public @NotNull ITask<Boolean> containsUserAsync(@NotNull UUID uniqueId) {
        return this.permissionManagementSupplier.get().containsUserAsync(uniqueId);
    }

    @Override
    public @NotNull ITask<Boolean> containsUserAsync(@NotNull String name) {
        return this.permissionManagementSupplier.get().containsUserAsync(name);
    }

    @Override
    public @NotNull ITask<IPermissionUser> getUserAsync(@NotNull UUID uniqueId) {
        return this.permissionManagementSupplier.get().getUserAsync(uniqueId);
    }

    @Override
    public @NotNull ITask<List<IPermissionUser>> getUsersAsync(@NotNull String name) {
        return this.permissionManagementSupplier.get().getUsersAsync(name);
    }

    @Override
    public @NotNull ITask<Collection<IPermissionUser>> getUsersAsync() {
        return this.permissionManagementSupplier.get().getUsersAsync();
    }

    @Override
    public @NotNull ITask<Void> setUsersAsync(@NotNull Collection<? extends IPermissionUser> users) {
        return this.permissionManagementSupplier.get().setUsersAsync(users);
    }

    @Override
    public @NotNull ITask<Collection<IPermissionUser>> getUsersByGroupAsync(@NotNull String group) {
        return this.permissionManagementSupplier.get().getUsersByGroupAsync(group);
    }

    @Override
    public @NotNull ITask<Void> addGroupAsync(@NotNull IPermissionGroup permissionGroup) {
        return ((ListenableTask<IPermissionGroup>) this.permissionManagementSupplier.get().addGroupAsync(permissionGroup)).map(o -> null);
    }

    @Override
    public @NotNull ITask<Void> updateGroupAsync(@NotNull IPermissionGroup permissionGroup) {
        return this.permissionManagementSupplier.get().updateGroupAsync(permissionGroup);
    }

    @Override
    public @NotNull ITask<Void> deleteGroupAsync(@NotNull String name) {
        return this.permissionManagementSupplier.get().deleteGroupAsync(name);
    }

    @Override
    public @NotNull ITask<Void> deleteGroupAsync(@NotNull IPermissionGroup permissionGroup) {
        return this.permissionManagementSupplier.get().deleteGroupAsync(permissionGroup);
    }

    @Override
    public @NotNull ITask<Boolean> containsGroupAsync(@NotNull String group) {
        return this.permissionManagementSupplier.get().containsGroupAsync(group);
    }

    @Override
    public @NotNull ITask<IPermissionGroup> getGroupAsync(@NotNull String name) {
        return this.permissionManagementSupplier.get().getGroupAsync(name);
    }

    @Override
    public @NotNull ITask<Collection<IPermissionGroup>> getGroupsAsync() {
        return this.permissionManagementSupplier.get().getGroupsAsync();
    }

    @Override
    public @NotNull ITask<Void> setGroupsAsync(@NotNull Collection<? extends IPermissionGroup> groups) {
        return this.permissionManagementSupplier.get().setGroupsAsync(groups);
    }
}
