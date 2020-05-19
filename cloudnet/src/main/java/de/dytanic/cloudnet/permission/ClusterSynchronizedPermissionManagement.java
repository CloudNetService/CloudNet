package de.dytanic.cloudnet.permission;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.NullCompletableTask;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public abstract class ClusterSynchronizedPermissionManagement implements NodePermissionManagement {
    @Override
    public @NotNull ITask<IPermissionUser> addUserAsync(@NotNull IPermissionUser permissionUser) {
        Preconditions.checkNotNull(permissionUser);
        if (this.getPermissionManagementHandler() != null) {
            this.getPermissionManagementHandler().handleAddUser(this, permissionUser);
        }
        return this.addUserWithoutClusterSyncAsync(permissionUser);
    }

    @Override
    public @NotNull ITask<Void> updateUserAsync(@NotNull IPermissionUser permissionUser) {
        Preconditions.checkNotNull(permissionUser);
        if (this.getPermissionManagementHandler() != null) {
            this.getPermissionManagementHandler().handleUpdateUser(this, permissionUser);
        }
        return this.updateUserWithoutClusterSyncAsync(permissionUser);
    }

    @Override
    public @NotNull ITask<Boolean> deleteUserAsync(@NotNull String name) {
        Preconditions.checkNotNull(name);

        CompletableTask<Boolean> task = new CompletableTask<>();

        this.getUsersAsync(name).onComplete(users -> {
            boolean success = !users.isEmpty();
            for (IPermissionUser user : users) {
                success = success && this.deleteUserAsync(user).getDef(false);
            }
            task.complete(success);
        }).onCancelled(listITask -> task.cancel(true)).onFailure(throwable -> task.complete(false));

        return task;
    }

    @Override
    public @NotNull ITask<Boolean> deleteUserAsync(@NotNull IPermissionUser permissionUser) {
        Preconditions.checkNotNull(permissionUser);
        if (this.getPermissionManagementHandler() != null) {
            this.getPermissionManagementHandler().handleDeleteUser(this, permissionUser);
        }
        return this.deleteUserWithoutClusterSyncAsync(permissionUser);
    }

    @Override
    public @NotNull ITask<Void> setUsersAsync(@Nullable Collection<? extends IPermissionUser> users) {
        if (users == null) {
            users = Collections.emptyList();
        }
        if (this.getPermissionManagementHandler() != null) {
            this.getPermissionManagementHandler().handleSetUsers(this, users);
        }

        return this.setUsersWithoutClusterSyncAsync(users);
    }

    @Override
    public @NotNull ITask<IPermissionGroup> addGroupAsync(@NotNull IPermissionGroup permissionGroup) {
        Preconditions.checkNotNull(permissionGroup);

        this.testPermissionGroup(permissionGroup);

        if (this.getPermissionManagementHandler() != null) {
            this.getPermissionManagementHandler().handleAddGroup(this, permissionGroup);
        }
        return this.addGroupWithoutClusterSyncAsync(permissionGroup);
    }

    @Override
    public @NotNull ITask<Void> updateGroupAsync(@NotNull IPermissionGroup permissionGroup) {
        Preconditions.checkNotNull(permissionGroup);

        this.testPermissionGroup(permissionGroup);

        if (this.getPermissionManagementHandler() != null) {
            this.getPermissionManagementHandler().handleUpdateGroup(this, permissionGroup);
        }
        return this.updateGroupWithoutClusterSyncAsync(permissionGroup);
    }

    @Override
    public @NotNull ITask<Void> deleteGroupAsync(@NotNull IPermissionGroup permissionGroup) {
        if (this.getPermissionManagementHandler() != null) {
            this.getPermissionManagementHandler().handleDeleteGroup(this, permissionGroup);
        }

        return this.deleteGroupWithoutClusterSyncAsync(permissionGroup);
    }

    @Override
    public @NotNull ITask<Void> deleteGroupAsync(@NotNull String name) {
        CompletableTask<Void> task = new NullCompletableTask<>();
        this.getGroupAsync(name).onComplete(permissionGroup -> {
            if (permissionGroup == null) {
                task.call();
                return;
            }
            this.deleteGroupAsync(permissionGroup).onComplete(aVoid -> task.call());
        });
        return task;
    }

    @Override
    public @NotNull ITask<Void> setGroupsAsync(Collection<? extends IPermissionGroup> groups) {
        if (groups == null) {
            groups = Collections.emptyList();
        }
        if (this.getPermissionManagementHandler() != null) {
            this.getPermissionManagementHandler().handleSetGroups(this, groups);
        }
        return this.setGroupsWithoutClusterSyncAsync(groups);
    }

    public void setGroups(Collection<? extends IPermissionGroup> groups) {
        if (groups == null) {
            groups = Collections.emptyList();
        }
        if (this.getPermissionManagementHandler() != null) {
            this.getPermissionManagementHandler().handleSetGroups(this, groups);
        }
        this.setGroupsWithoutClusterSyncAsync(groups);
    }

    public abstract ITask<IPermissionUser> addUserWithoutClusterSyncAsync(IPermissionUser permissionUser);

    public abstract ITask<Void> updateUserWithoutClusterSyncAsync(IPermissionUser permissionUser);

    public abstract ITask<Boolean> deleteUserWithoutClusterSyncAsync(IPermissionUser permissionUser);

    public abstract ITask<Void> setUsersWithoutClusterSyncAsync(Collection<? extends IPermissionUser> users);

    public abstract ITask<IPermissionGroup> addGroupWithoutClusterSyncAsync(IPermissionGroup permissionGroup);

    public abstract ITask<Void> updateGroupWithoutClusterSyncAsync(IPermissionGroup permissionGroup);

    public abstract ITask<Void> deleteGroupWithoutClusterSyncAsync(String group);

    public abstract ITask<Void> deleteGroupWithoutClusterSyncAsync(IPermissionGroup group);

    public abstract ITask<Void> setGroupsWithoutClusterSyncAsync(Collection<? extends IPermissionGroup> groups);

}
