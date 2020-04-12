package de.dytanic.cloudnet.ext.cloudperms;

import de.dytanic.cloudnet.common.concurrent.CompletedTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.permission.*;
import de.dytanic.cloudnet.ext.cloudperms.listener.PermissionsUpdateListener;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CloudPermissionsManagement implements DefaultPermissionManagement, DefaultSynchronizedPermissionManagement {

    private final Map<String, IPermissionGroup> cachedPermissionGroups = new ConcurrentHashMap<>();
    private final Map<UUID, IPermissionUser> cachedPermissionUsers = new ConcurrentHashMap<>();

    private final IPermissionManagement childPermissionManagement;

    protected CloudPermissionsManagement(@NotNull IPermissionManagement childPermissionManagement) {
        this.childPermissionManagement = childPermissionManagement;
        this.init();

        CloudNetDriver.getInstance().setPermissionManagement(this);
    }

    /**
     * @deprecated use {@link CloudNetDriver#getPermissionManagement()} instead.
     */
    @Deprecated
    public static CloudPermissionsManagement getInstance() {
        return (CloudPermissionsManagement) CloudNetDriver.getInstance().getPermissionManagement();
    }

    public static CloudPermissionsManagement newInstance() {
        return new CloudPermissionsPermissionManagement(Objects.requireNonNull(CloudNetDriver.getInstance().getPermissionManagement()));
    }

    private void init() {
        for (IPermissionGroup permissionGroup : this.getChildPermissionManagement().getGroups()) {
            this.cachedPermissionGroups.put(permissionGroup.getName(), permissionGroup);
        }

        this.getDriver().getEventManager().registerListener(new PermissionsUpdateListener(this));
    }

    public boolean hasPlayerPermission(IPermissionUser permissionUser, String perm) {
        Permission permission = new Permission(perm, 0);

        for (String group : Wrapper.getInstance().getServiceConfiguration().getGroups()) {
            if (hasPermission(permissionUser, group, permission)) {
                return true;
            }
        }

        return hasPermission(permissionUser, permission);
    }

    @Override
    public @NotNull IPermissionManagement getChildPermissionManagement() {
        return this.childPermissionManagement;
    }

    @Override
    public @NotNull ITask<IPermissionUser> addUserAsync(@NotNull IPermissionUser permissionUser) {
        return this.childPermissionManagement.addUserAsync(permissionUser);
    }

    @Override
    public @NotNull ITask<IPermissionUser> addUserAsync(@NotNull String name, @NotNull String password, int potency) {
        return this.childPermissionManagement.addUserAsync(name, password, potency);
    }

    @Override
    public @NotNull ITask<Void> updateUserAsync(@NotNull IPermissionUser permissionUser) {
        return this.childPermissionManagement.updateUserAsync(permissionUser);
    }

    @Override
    public @NotNull ITask<Boolean> deleteUserAsync(@NotNull String name) {
        return this.childPermissionManagement.deleteUserAsync(name);
    }

    @Override
    public @NotNull ITask<Boolean> deleteUserAsync(@NotNull IPermissionUser permissionUser) {
        return this.childPermissionManagement.deleteUserAsync(permissionUser);
    }

    @Override
    public @NotNull ITask<Boolean> containsUserAsync(@NotNull String name) {
        if (this.cachedPermissionUsers.values().stream().anyMatch(permissionUser -> permissionUser.getName().equals(name))) {
            return CompletedTask.create(true);
        }
        return this.childPermissionManagement.containsUserAsync(name);
    }

    @Override
    public @NotNull ITask<Boolean> containsUserAsync(@NotNull UUID uniqueId) {
        if (this.cachedPermissionUsers.containsKey(uniqueId)) {
            return CompletedTask.create(true);
        }
        return this.childPermissionManagement.containsUserAsync(uniqueId);
    }

    @Override
    public @NotNull ITask<IPermissionUser> getUserAsync(@NotNull UUID uniqueId) {
        if (this.cachedPermissionUsers.containsKey(uniqueId)) {
            return CompletedTask.create(this.cachedPermissionUsers.get(uniqueId));
        }
        return this.childPermissionManagement.getUserAsync(uniqueId);
    }

    @Override
    public @NotNull ITask<List<IPermissionUser>> getUsersAsync(@NotNull String name) {
        return this.childPermissionManagement.getUsersAsync(name);
    }

    @Override
    public @NotNull ITask<IPermissionUser> getFirstUserAsync(String name) {
        return this.childPermissionManagement.getFirstUserAsync(name);
    }

    @Override
    public @NotNull ITask<Collection<IPermissionUser>> getUsersAsync() {
        return this.childPermissionManagement.getUsersAsync();
    }

    @Override
    public @NotNull ITask<Void> setUsersAsync(@Nullable Collection<? extends IPermissionUser> users) {
        return this.childPermissionManagement.setUsersAsync(users);
    }

    @Override
    public @NotNull ITask<Collection<IPermissionUser>> getUsersByGroupAsync(@NotNull String group) {
        return this.childPermissionManagement.getUsersByGroupAsync(group);
    }

    @Override
    public @NotNull ITask<IPermissionGroup> addGroupAsync(@NotNull String role, int potency) {
        return this.childPermissionManagement.addGroupAsync(role, potency);
    }

    @Override
    public @NotNull ITask<IPermissionGroup> addGroupAsync(@NotNull IPermissionGroup permissionGroup) {
        return this.childPermissionManagement.addGroupAsync(permissionGroup);
    }

    @Override
    public @NotNull ITask<Void> updateGroupAsync(@NotNull IPermissionGroup permissionGroup) {
        return this.childPermissionManagement.updateGroupAsync(permissionGroup);
    }

    @Override
    public @NotNull ITask<Void> deleteGroupAsync(@NotNull String name) {
        return this.childPermissionManagement.deleteGroupAsync(name);
    }

    @Override
    public @NotNull ITask<Void> deleteGroupAsync(@NotNull IPermissionGroup permissionGroup) {
        return this.childPermissionManagement.deleteGroupAsync(permissionGroup);
    }

    @Override
    public @NotNull ITask<Boolean> containsGroupAsync(@NotNull String group) {
        return CompletedTask.create(this.cachedPermissionGroups.containsKey(group));
    }

    @Override
    public @NotNull ITask<IPermissionGroup> getGroupAsync(@NotNull String name) {
        return CompletedTask.create(this.cachedPermissionGroups.get(name));
    }

    @Override
    public @NotNull ITask<IPermissionGroup> getDefaultPermissionGroupAsync() {
        return CompletedTask.create(this.cachedPermissionGroups.values().stream()
                .filter(IPermissionGroup::isDefaultGroup)
                .findFirst()
                .orElse(null));
    }

    @Override
    public @NotNull ITask<Collection<IPermissionGroup>> getGroupsAsync() {
        return CompletedTask.create(this.cachedPermissionGroups.values());
    }

    @Override
    public Collection<IPermissionGroup> getGroups() {
        return this.cachedPermissionGroups.values();
    }

    @Override
    public @NotNull ITask<Void> setGroupsAsync(@Nullable Collection<? extends IPermissionGroup> groups) {
        return this.childPermissionManagement.setGroupsAsync(groups);
    }

    @Override
    public boolean reload() {
        this.childPermissionManagement.reload();

        Collection<IPermissionGroup> permissionGroups = this.childPermissionManagement.getGroups();

        this.cachedPermissionGroups.clear();

        for (IPermissionGroup group : permissionGroups) {
            this.cachedPermissionGroups.put(group.getName(), group);
        }

        return true;
    }

    @Override
    public @NotNull ITask<Collection<IPermissionGroup>> getGroupsAsync(@Nullable IPermissionUser permissionUser) {
        return this.childPermissionManagement.getGroupsAsync(permissionUser);
    }


    private CloudNetDriver getDriver() {
        return CloudNetDriver.getInstance();
    }

    public Map<String, IPermissionGroup> getCachedPermissionGroups() {
        return this.cachedPermissionGroups;
    }

    public Map<UUID, IPermissionUser> getCachedPermissionUsers() {
        return this.cachedPermissionUsers;
    }
}