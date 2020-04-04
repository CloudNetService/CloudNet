package de.dytanic.cloudnet.ext.cloudperms;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.permission.*;
import de.dytanic.cloudnet.ext.cloudperms.listener.PermissionsUpdateListener;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class CloudPermissionsManagement implements DefaultPermissionManagement, DefaultSynchronizedPermissionManagement {

    private static CloudPermissionsManagement instance;
    private final Map<String, IPermissionGroup> cachedPermissionGroups = new ConcurrentHashMap<>();
    private final Map<UUID, IPermissionUser> cachedPermissionUsers = new ConcurrentHashMap<>();

    private final IPermissionManagement childPermissionManagement;

    protected CloudPermissionsManagement(IPermissionManagement childPermissionManagement) {
        this.childPermissionManagement = childPermissionManagement;
        this.init();

        CloudNetDriver.getInstance().setPermissionManagement(this);
    }

    @Deprecated
    public static CloudPermissionsManagement getInstance() {
        return CloudPermissionsManagement.instance != null
                ? CloudPermissionsManagement.instance
                : (CloudPermissionsManagement.instance = new CloudPermissionsPermissionManagement(CloudNetDriver.getInstance().getPermissionManagement()));
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
    public IPermissionManagement getChildPermissionManagement() {
        return this.childPermissionManagement;
    }

    @NotNull
    @Deprecated
    public IPermissionManagementHandler getPermissionManagementHandler() {
        throw new UnsupportedOperationException("PermissionManagementHandler is not available in this implementation");
    }

    @Deprecated
    public void setPermissionManagementHandler(@NotNull IPermissionManagementHandler permissionManagementHandler) {
        throw new UnsupportedOperationException("PermissionManagementHandler is not available in this implementation");
    }

    @NotNull
    @Override
    public IPermissionUser addUser(@NotNull IPermissionUser permissionUser) {
        return this.childPermissionManagement.addUser(permissionUser);
    }

    @Override
    public void updateUser(@NotNull IPermissionUser permissionUser) {
        this.getChildPermissionManagement().updateUser(permissionUser);
    }

    @Override
    public void deleteUser(@NotNull String name) {
        this.getChildPermissionManagement().deleteUser(name);
    }

    @Override
    public void deleteUser(@NotNull IPermissionUser permissionUser) {
        this.getChildPermissionManagement().deleteUser(permissionUser);
    }

    @Override
    public boolean containsUser(@NotNull UUID uniqueId) {
        return this.cachedPermissionUsers.containsKey(uniqueId) || this.getChildPermissionManagement().containsUser(uniqueId);
    }

    @Override
    public boolean containsUser(@NotNull String name) {
        return this.getChildPermissionManagement().containsUser(name);
    }

    @Nullable
    @Override
    public IPermissionUser getUser(@NotNull UUID uniqueId) {
        return this.cachedPermissionUsers.containsKey(uniqueId) ? this.cachedPermissionUsers.get(uniqueId) : this.getChildPermissionManagement().getUser(uniqueId);
    }

    @Override
    public List<IPermissionUser> getUsers(@NotNull String name) {
        return this.getChildPermissionManagement().getUsers(name);
    }

    @Override
    public Collection<IPermissionUser> getUsers() {
        return this.getChildPermissionManagement().getUsers();
    }

    @Override
    public void setUsers(@Nullable Collection<? extends IPermissionUser> users) {
        this.getChildPermissionManagement().setUsers(users);
    }

    @Override
    public Collection<IPermissionUser> getUsersByGroup(@NotNull String group) {
        return this.getChildPermissionManagement().getUsersByGroup(group);
    }

    @Override
    public IPermissionGroup addGroup(@NotNull IPermissionGroup permissionGroup) {
        this.getChildPermissionManagement().addGroup(permissionGroup);

        return permissionGroup;
    }

    @Override
    public void updateGroup(@NotNull IPermissionGroup permissionGroup) {
        this.getChildPermissionManagement().updateGroup(permissionGroup);
    }

    @Override
    public void deleteGroup(@NotNull String group) {
        this.getChildPermissionManagement().deleteGroup(group);
    }

    @Override
    public void deleteGroup(@NotNull IPermissionGroup group) {
        this.getChildPermissionManagement().deleteGroup(group);
    }

    @Override
    public boolean containsGroup(@NotNull String name) {
        return this.cachedPermissionGroups.containsKey(name);
    }

    @Nullable
    @Override
    public IPermissionGroup getGroup(@NotNull String name) {
        Preconditions.checkNotNull(name);

        return this.cachedPermissionGroups.get(name);
    }

    @Override
    public Collection<IPermissionGroup> getGroups() {
        return this.cachedPermissionGroups.values();
    }

    @Override
    public void setGroups(@NotNull Collection<? extends IPermissionGroup> groups) {
        this.getChildPermissionManagement().setGroups(groups);
    }

    @Override
    public boolean reload() {
        Collection<IPermissionGroup> permissionGroups = this.getChildPermissionManagement().getGroups();

        this.cachedPermissionGroups.clear();

        for (IPermissionGroup group : permissionGroups) {
            this.cachedPermissionGroups.put(group.getName(), group);
        }

        return true;
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

    @Override
    public <V> ITask<V> scheduleTask(Callable<V> callable) {
        return this.getDriver().getTaskScheduler().schedule(callable);
    }
}