package de.dytanic.cloudnet.ext.cloudperms;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Maps;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.permission.*;
import de.dytanic.cloudnet.ext.cloudperms.listener.PermissionsUpdateListener;
import de.dytanic.cloudnet.wrapper.Wrapper;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CloudPermissionsManagement implements IPermissionManagement {

    private static CloudPermissionsManagement instance;
    private final Map<String, IPermissionGroup> cachedPermissionGroups = Maps.newConcurrentHashMap();
    private final Map<UUID, IPermissionUser> cachedPermissionUsers = Maps.newConcurrentHashMap();

    CloudPermissionsManagement() {
        this.init();
    }

    public static CloudPermissionsManagement getInstance() {
        return CloudPermissionsManagement.instance != null
                ? CloudPermissionsManagement.instance
                : (CloudPermissionsManagement.instance = new CloudPermissionsPermissionManagement());
    }

    private void init() {
        for (IPermissionGroup permissionGroup : this.getDriver().getPermissionProvider().getGroups()) {
            this.cachedPermissionGroups.put(permissionGroup.getName(), permissionGroup);
        }

        this.getDriver().getEventManager().registerListener(new PermissionsUpdateListener());
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
    public IPermissionManagementHandler getPermissionManagementHandler() {
        throw new UnsupportedOperationException("PermissionManagementHandler is not available in this implementation");
    }

    @Override
    public void setPermissionManagementHandler(IPermissionManagementHandler permissionManagementHandler) {
        throw new UnsupportedOperationException("PermissionManagementHandler is not available in this implementation");
    }

    @Override
    public IPermissionUser addUser(IPermissionUser permissionUser) {
        Validate.checkNotNull(permissionUser);

        try {
            this.getDriver().getPermissionProvider().addUserAsync(permissionUser).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }

        return permissionUser;
    }

    @Override
    public void updateUser(IPermissionUser permissionUser) {
        Validate.checkNotNull(permissionUser);

        this.getDriver().getPermissionProvider().updateUser(permissionUser);
    }

    @Override
    public void deleteUser(String name) {
        Validate.checkNotNull(name);

        this.getDriver().getPermissionProvider().deleteUser(name);
    }

    @Override
    public void deleteUser(IPermissionUser permissionUser) {
        Validate.checkNotNull(permissionUser);

        this.getDriver().getPermissionProvider().deleteUser(permissionUser);
    }

    @Override
    public boolean containsUser(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        return this.cachedPermissionUsers.containsKey(uniqueId) || this.getDriver().getPermissionProvider().containsUser(uniqueId);
    }

    @Override
    public boolean containsUser(String name) {
        Validate.checkNotNull(name);

        return this.getDriver().getPermissionProvider().containsUser(name);
    }

    @Override
    public IPermissionUser getUser(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        return this.cachedPermissionUsers.containsKey(uniqueId) ? this.cachedPermissionUsers.get(uniqueId) : this.getDriver().getPermissionProvider().getUser(uniqueId);
    }

    @Override
    public List<IPermissionUser> getUsers(String name) {
        Validate.checkNotNull(name);

        return this.getDriver().getPermissionProvider().getUsers(name);
    }

    @Override
    public Collection<IPermissionUser> getUsers() {
        return this.getDriver().getPermissionProvider().getUsers();
    }

    @Override
    public void setUsers(Collection<? extends IPermissionUser> users) {
        Validate.checkNotNull(users);

        this.getDriver().getPermissionProvider().setUsers(users);
    }

    @Override
    public Collection<IPermissionUser> getUsersByGroup(String group) {
        Validate.checkNotNull(group);

        return this.getDriver().getPermissionProvider().getUsersByGroup(group);
    }

    @Override
    public IPermissionGroup addGroup(IPermissionGroup permissionGroup) {
        Validate.checkNotNull(permissionGroup);

        this.getDriver().getPermissionProvider().addGroup(permissionGroup);

        return permissionGroup;
    }

    @Override
    public void updateGroup(IPermissionGroup permissionGroup) {
        Validate.checkNotNull(permissionGroup);

        this.getDriver().getPermissionProvider().updateGroup(permissionGroup);
    }

    @Override
    public void deleteGroup(String group) {
        Validate.checkNotNull(group);

        this.getDriver().getPermissionProvider().deleteGroup(group);
    }

    @Override
    public void deleteGroup(IPermissionGroup group) {
        Validate.checkNotNull(group);

        this.getDriver().getPermissionProvider().deleteGroup(group);
    }

    @Override
    public boolean containsGroup(String name) {
        Validate.checkNotNull(name);

        return this.cachedPermissionGroups.containsKey(name);
    }

    @Override
    public IPermissionGroup getGroup(String name) {
        Validate.checkNotNull(name);

        return this.cachedPermissionGroups.get(name);
    }

    @Override
    public Collection<IPermissionGroup> getGroups() {
        return this.cachedPermissionGroups.values();
    }

    @Override
    public void setGroups(Collection<? extends IPermissionGroup> groups) {
        Validate.checkNotNull(groups);

        this.getDriver().getPermissionProvider().setGroups(groups);
    }

    @Override
    public boolean reload() {
        Collection<IPermissionGroup> permissionGroups = this.getDriver().getPermissionProvider().getGroups();

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

}
