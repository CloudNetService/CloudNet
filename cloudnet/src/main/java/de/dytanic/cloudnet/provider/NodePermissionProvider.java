package de.dytanic.cloudnet.provider;

import de.dytanic.cloudnet.CloudNet;
import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.driver.provider.PermissionProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class NodePermissionProvider implements PermissionProvider {

    private CloudNet cloudNet;

    public NodePermissionProvider(CloudNet cloudNet) {
        this.cloudNet = cloudNet;
    }

    private IPermissionManagement getPermissionManagement() {
        return this.cloudNet.getPermissionManagement();
    }

    @Override
    public void addUser(@NotNull IPermissionUser permissionUser) {
        Preconditions.checkNotNull(permissionUser);

        this.getPermissionManagement().addUser(permissionUser);
    }

    @Override
    public void updateUser(@NotNull IPermissionUser permissionUser) {
        Preconditions.checkNotNull(permissionUser);

        this.getPermissionManagement().updateUser(permissionUser);
    }

    @Override
    public void deleteUser(@NotNull String name) {
        Preconditions.checkNotNull(name);

        this.getPermissionManagement().deleteUser(name);
    }

    @Override
    public void deleteUser(@NotNull IPermissionUser permissionUser) {
        Preconditions.checkNotNull(permissionUser);

        this.getPermissionManagement().deleteUser(permissionUser);
    }

    @Override
    public boolean containsUser(@NotNull UUID uniqueId) {
        Preconditions.checkNotNull(uniqueId);

        return this.getPermissionManagement().containsUser(uniqueId);
    }

    @Override
    public boolean containsUser(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return this.getPermissionManagement().containsUser(name);
    }

    @Nullable
    @Override
    public IPermissionUser getUser(@NotNull UUID uniqueId) {
        Preconditions.checkNotNull(uniqueId);

        return this.getPermissionManagement().getUser(uniqueId);
    }

    @Override
    public List<IPermissionUser> getUsers(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return this.getPermissionManagement().getUsers(name);
    }

    @Override
    public Collection<IPermissionUser> getUsers() {
        return this.getPermissionManagement().getUsers();
    }

    @Override
    public void setUsers(@NotNull Collection<? extends IPermissionUser> users) {
        Preconditions.checkNotNull(users);

        this.getPermissionManagement().setUsers(users);
    }

    @Override
    public Collection<IPermissionUser> getUsersByGroup(@NotNull String group) {
        Preconditions.checkNotNull(group);

        return this.getPermissionManagement().getUsersByGroup(group);
    }

    @Override
    public void addGroup(@NotNull IPermissionGroup permissionGroup) {
        Preconditions.checkNotNull(permissionGroup);

        this.getPermissionManagement().addGroup(permissionGroup);
    }

    @Override
    public void updateGroup(@NotNull IPermissionGroup permissionGroup) {
        Preconditions.checkNotNull(permissionGroup);

        this.getPermissionManagement().updateGroup(permissionGroup);
    }

    @Override
    public void deleteGroup(@NotNull String group) {
        Preconditions.checkNotNull(group);

        this.getPermissionManagement().deleteGroup(group);
    }

    @Override
    public void deleteGroup(@NotNull IPermissionGroup group) {
        Preconditions.checkNotNull(group);

        this.getPermissionManagement().deleteGroup(group);
    }

    @Override
    public boolean containsGroup(@NotNull String group) {
        Preconditions.checkNotNull(group);

        return this.getPermissionManagement().containsGroup(group);
    }

    @Nullable
    @Override
    public IPermissionGroup getGroup(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return this.getPermissionManagement().getGroup(name);
    }

    @Override
    public Collection<IPermissionGroup> getGroups() {
        return this.getPermissionManagement().getGroups();
    }

    @Override
    public void setGroups(@NotNull Collection<? extends IPermissionGroup> groups) {
        Preconditions.checkNotNull(groups);

        this.getPermissionManagement().setGroups(groups);
    }

    @Override
    public ITask<Void> addUserAsync(@NotNull IPermissionUser permissionUser) {
        Preconditions.checkNotNull(permissionUser);

        return this.cloudNet.scheduleTask(() -> null);
    }

    @Override
    public ITask<Void> updateUserAsync(@NotNull IPermissionUser permissionUser) {
        return this.cloudNet.scheduleTask(() -> {
            this.updateUser(permissionUser);
            return null;
        });
    }

    @Override
    public ITask<Void> deleteUserAsync(@NotNull String name) {
        return this.cloudNet.scheduleTask(() -> {
            this.deleteUser(name);
            return null;
        });
    }

    @Override
    public ITask<Void> deleteUserAsync(@NotNull IPermissionUser permissionUser) {
        return this.cloudNet.scheduleTask(() -> {
            this.deleteUser(permissionUser);
            return null;
        });
    }

    @Override
    public ITask<Boolean> containsUserAsync(@NotNull UUID uniqueId) {
        Preconditions.checkNotNull(uniqueId);

        return this.cloudNet.scheduleTask(() -> this.containsUser(uniqueId));
    }

    @Override
    public ITask<Boolean> containsUserAsync(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return this.cloudNet.scheduleTask(() -> this.containsUser(name));
    }

    @Override
    public ITask<IPermissionUser> getUserAsync(@NotNull UUID uniqueId) {
        Preconditions.checkNotNull(uniqueId);

        return this.cloudNet.scheduleTask(() -> this.getUser(uniqueId));
    }

    @Override
    public ITask<List<IPermissionUser>> getUsersAsync(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return this.cloudNet.scheduleTask(() -> this.getUsers(name));
    }

    @Override
    public ITask<Collection<IPermissionUser>> getUsersAsync() {
        return this.cloudNet.scheduleTask(this::getUsers);
    }

    @Override
    public ITask<Void> setUsersAsync(@NotNull Collection<? extends IPermissionUser> users) {
        return this.cloudNet.scheduleTask(() -> {
            this.setUsers(users);
            return null;
        });
    }

    @Override
    public ITask<Collection<IPermissionUser>> getUsersByGroupAsync(@NotNull String group) {
        Preconditions.checkNotNull(group);

        return this.cloudNet.scheduleTask(() -> this.getUsersByGroup(group));
    }

    @Override
    public ITask<Void> addGroupAsync(@NotNull IPermissionGroup permissionGroup) {
        return this.cloudNet.scheduleTask(() -> {
            this.addGroup(permissionGroup);
            return null;
        });
    }

    @Override
    public ITask<Void> updateGroupAsync(@NotNull IPermissionGroup permissionGroup) {
        return this.cloudNet.scheduleTask(() -> {
            this.updateGroup(permissionGroup);
            return null;
        });
    }

    @Override
    public ITask<Void> deleteGroupAsync(@NotNull String name) {
        return this.cloudNet.scheduleTask(() -> {
            this.deleteGroup(name);
            return null;
        });
    }

    @Override
    public ITask<Void> deleteGroupAsync(@NotNull IPermissionGroup permissionGroup) {
        return this.cloudNet.scheduleTask(() -> {
            this.deleteGroup(permissionGroup);
            return null;
        });
    }

    @Override
    public ITask<Boolean> containsGroupAsync(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return this.cloudNet.scheduleTask(() -> this.containsGroup(name));
    }

    @Override
    public ITask<IPermissionGroup> getGroupAsync(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return this.cloudNet.scheduleTask(() -> this.getGroup(name));
    }

    @Override
    public ITask<Collection<IPermissionGroup>> getGroupsAsync() {
        return this.cloudNet.scheduleTask(this::getGroups);
    }

    @Override
    public ITask<Void> setGroupsAsync(@NotNull Collection<? extends IPermissionGroup> groups) {
        return this.cloudNet.scheduleTask(() -> {
            this.setGroups(groups);
            return null;
        });
    }

}
