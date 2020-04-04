package de.dytanic.cloudnet.driver.permission;

import de.dytanic.cloudnet.common.concurrent.ITask;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

public interface DefaultSynchronizedPermissionManagement extends IPermissionManagement {

    <V> ITask<V> scheduleTask(Callable<V> callable);

    @Override
    default @NotNull ITask<IPermissionUser> addUserAsync(@NotNull IPermissionUser permissionUser) {
        return this.scheduleTask(() -> this.addUser(permissionUser));
    }

    @Override
    default @NotNull ITask<Void> updateUserAsync(@NotNull IPermissionUser permissionUser) {
        return this.scheduleTask(() -> {
            this.updateUser(permissionUser);
            return null;
        });
    }

    @Override
    default @NotNull ITask<Void> deleteUserAsync(@NotNull String name) {
        return this.scheduleTask(() -> {
            this.deleteUser(name);
            return null;
        });
    }

    @Override
    default @NotNull ITask<Void> deleteUserAsync(@NotNull IPermissionUser permissionUser) {
        return this.scheduleTask(() -> {
            this.deleteUser(permissionUser);
            return null;
        });
    }

    @Override
    default @NotNull ITask<Boolean> containsUserAsync(@NotNull UUID uniqueId) {
        return this.scheduleTask(() -> this.containsUser(uniqueId));
    }

    @Override
    default @NotNull ITask<Boolean> containsUserAsync(@NotNull String name) {
        return this.scheduleTask(() -> this.containsUser(name));
    }

    @Override
    default @NotNull ITask<IPermissionUser> getUserAsync(@NotNull UUID uniqueId) {
        return this.scheduleTask(() -> this.getUser(uniqueId));
    }

    @Override
    default @NotNull ITask<List<IPermissionUser>> getUsersAsync(@NotNull String name) {
        return this.scheduleTask(() -> this.getUsers(name));
    }

    @Override
    default @NotNull ITask<Collection<IPermissionUser>> getUsersAsync() {
        return this.scheduleTask(this::getUsers);
    }

    @Override
    default @NotNull ITask<Void> setUsersAsync(@NotNull Collection<? extends IPermissionUser> users) {
        return this.scheduleTask(() -> {
            this.setUsers(users);
            return null;
        });
    }

    @Override
    default @NotNull ITask<Collection<IPermissionUser>> getUsersByGroupAsync(@NotNull String group) {
        return this.scheduleTask(() -> this.getUsersByGroup(group));
    }

    @Override
    default @NotNull ITask<IPermissionGroup> addGroupAsync(@NotNull IPermissionGroup permissionGroup) {
        return this.scheduleTask(() -> this.addGroup(permissionGroup));
    }

    @Override
    default @NotNull ITask<Void> updateGroupAsync(@NotNull IPermissionGroup permissionGroup) {
        return this.scheduleTask(() -> {
            this.updateGroup(permissionGroup);
            return null;
        });
    }

    @Override
    default @NotNull ITask<Void> deleteGroupAsync(@NotNull String name) {
        return this.scheduleTask(() -> {
            this.deleteGroup(name);
            return null;
        });
    }

    @Override
    default @NotNull ITask<Void> deleteGroupAsync(@NotNull IPermissionGroup permissionGroup) {
        return this.scheduleTask(() -> {
            this.deleteGroup(permissionGroup);
            return null;
        });
    }

    @Override
    default @NotNull ITask<Boolean> containsGroupAsync(@NotNull String group) {
        return this.scheduleTask(() -> this.containsGroup(group));
    }

    @Override
    default @NotNull ITask<IPermissionGroup> getGroupAsync(@NotNull String name) {
        return this.scheduleTask(() -> this.getGroup(name));
    }

    @Override
    default @NotNull ITask<Collection<IPermissionGroup>> getGroupsAsync() {
        return this.scheduleTask(this::getGroups);
    }

    @Override
    default @NotNull ITask<Void> setGroupsAsync(@NotNull Collection<? extends IPermissionGroup> groups) {
        return this.scheduleTask(() -> {
            this.setGroups(groups);
            return null;
        });
    }

}
