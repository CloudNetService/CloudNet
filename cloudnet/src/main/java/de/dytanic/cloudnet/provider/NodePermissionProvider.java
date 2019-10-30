package de.dytanic.cloudnet.provider;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.driver.provider.PermissionProvider;

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
    public void addUser(IPermissionUser permissionUser) {
        Validate.checkNotNull(permissionUser);

        this.getPermissionManagement().addUser(permissionUser);
    }

    @Override
    public void updateUser(IPermissionUser permissionUser) {
        Validate.checkNotNull(permissionUser);

        this.getPermissionManagement().updateUser(permissionUser);
    }

    @Override
    public void deleteUser(String name) {
        Validate.checkNotNull(name);

        this.getPermissionManagement().deleteUser(name);
    }

    @Override
    public void deleteUser(IPermissionUser permissionUser) {
        Validate.checkNotNull(permissionUser);

        this.getPermissionManagement().deleteUser(permissionUser);
    }

    @Override
    public boolean containsUser(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        return this.getPermissionManagement().containsUser(uniqueId);
    }

    @Override
    public boolean containsUser(String name) {
        Validate.checkNotNull(name);

        return this.getPermissionManagement().containsUser(name);
    }

    @Override
    public IPermissionUser getUser(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        return this.getPermissionManagement().getUser(uniqueId);
    }

    @Override
    public List<IPermissionUser> getUsers(String name) {
        Validate.checkNotNull(name);

        return this.getPermissionManagement().getUser(name);
    }

    @Override
    public Collection<IPermissionUser> getUsers() {
        return this.getPermissionManagement().getUsers();
    }

    @Override
    public void setUsers(Collection<? extends IPermissionUser> users) {
        Validate.checkNotNull(users);

        this.getPermissionManagement().setUsers(users);
    }

    @Override
    public Collection<IPermissionUser> getUsersByGroup(String group) {
        Validate.checkNotNull(group);

        return this.getPermissionManagement().getUserByGroup(group);
    }

    @Override
    public void addGroup(IPermissionGroup permissionGroup) {
        Validate.checkNotNull(permissionGroup);

        this.getPermissionManagement().addGroup(permissionGroup);
    }

    @Override
    public void updateGroup(IPermissionGroup permissionGroup) {
        Validate.checkNotNull(permissionGroup);

        this.getPermissionManagement().updateGroup(permissionGroup);
    }

    @Override
    public void deleteGroup(String group) {
        Validate.checkNotNull(group);

        this.getPermissionManagement().deleteGroup(group);
    }

    @Override
    public void deleteGroup(IPermissionGroup group) {
        Validate.checkNotNull(group);

        this.getPermissionManagement().deleteGroup(group);
    }

    @Override
    public boolean containsGroup(String group) {
        Validate.checkNotNull(group);

        return this.getPermissionManagement().containsGroup(group);
    }

    @Override
    public IPermissionGroup getGroup(String name) {
        Validate.checkNotNull(name);

        return this.getPermissionManagement().getGroup(name);
    }

    @Override
    public Collection<IPermissionGroup> getGroups() {
        return this.getPermissionManagement().getGroups();
    }

    @Override
    public void setGroups(Collection<? extends IPermissionGroup> groups) {
        Validate.checkNotNull(groups);

        this.getPermissionManagement().setGroups(groups);
    }

    @Override
    public ITask<Void> addUserAsync(IPermissionUser permissionUser) {
        Validate.checkNotNull(permissionUser);

        return this.cloudNet.scheduleTask(() -> null);
    }

    @Override
    public ITask<Void> updateUserAsync(IPermissionUser permissionUser) {
        return this.cloudNet.scheduleTask(() -> {
            this.updateUser(permissionUser);
            return null;
        });
    }

    @Override
    public ITask<Void> deleteUserAsync(String name) {
        return this.cloudNet.scheduleTask(() -> {
            this.deleteUser(name);
            return null;
        });
    }

    @Override
    public ITask<Void> deleteUserAsync(IPermissionUser permissionUser) {
        return this.cloudNet.scheduleTask(() -> {
            this.deleteUser(permissionUser);
            return null;
        });
    }

    @Override
    public ITask<Boolean> containsUserAsync(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        return this.cloudNet.scheduleTask(() -> this.containsUser(uniqueId));
    }

    @Override
    public ITask<Boolean> containsUserAsync(String name) {
        Validate.checkNotNull(name);

        return this.cloudNet.scheduleTask(() -> this.containsUser(name));
    }

    @Override
    public ITask<IPermissionUser> getUserAsync(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        return this.cloudNet.scheduleTask(() -> this.getUser(uniqueId));
    }

    @Override
    public ITask<List<IPermissionUser>> getUsersAsync(String name) {
        Validate.checkNotNull(name);

        return this.cloudNet.scheduleTask(() -> this.getUsers(name));
    }

    @Override
    public ITask<Collection<IPermissionUser>> getUsersAsync() {
        return this.cloudNet.scheduleTask(this::getUsers);
    }

    @Override
    public ITask<Void> setUsersAsync(Collection<? extends IPermissionUser> users) {
        return this.cloudNet.scheduleTask(() -> {
            this.setUsers(users);
            return null;
        });
    }

    @Override
    public ITask<Collection<IPermissionUser>> getUsersByGroupAsync(String group) {
        Validate.checkNotNull(group);

        return this.cloudNet.scheduleTask(() -> this.getUsersByGroup(group));
    }

    @Override
    public ITask<Void> addGroupAsync(IPermissionGroup permissionGroup) {
        return this.cloudNet.scheduleTask(() -> {
            this.addGroup(permissionGroup);
            return null;
        });
    }

    @Override
    public ITask<Void> updateGroupAsync(IPermissionGroup permissionGroup) {
        return this.cloudNet.scheduleTask(() -> {
            this.updateGroup(permissionGroup);
            return null;
        });
    }

    @Override
    public ITask<Void> deleteGroupAsync(String name) {
        return this.cloudNet.scheduleTask(() -> {
            this.deleteGroup(name);
            return null;
        });
    }

    @Override
    public ITask<Void> deleteGroupAsync(IPermissionGroup permissionGroup) {
        return this.cloudNet.scheduleTask(() -> {
            this.deleteGroup(permissionGroup);
            return null;
        });
    }

    @Override
    public ITask<Boolean> containsGroupAsync(String name) {
        Validate.checkNotNull(name);

        return this.cloudNet.scheduleTask(() -> this.containsGroup(name));
    }

    @Override
    public ITask<IPermissionGroup> getGroupAsync(String name) {
        Validate.checkNotNull(name);

        return this.cloudNet.scheduleTask(() -> this.getGroup(name));
    }

    @Override
    public ITask<Collection<IPermissionGroup>> getGroupsAsync() {
        return this.cloudNet.scheduleTask(this::getGroups);
    }

    @Override
    public ITask<Void> setGroupsAsync(Collection<? extends IPermissionGroup> groups) {
        return this.cloudNet.scheduleTask(() -> {
            this.setGroups(groups);
            return null;
        });
    }

}
