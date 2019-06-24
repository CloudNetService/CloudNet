package de.dytanic.cloudnet.ext.cloudperms;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Maps;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.permission.*;
import de.dytanic.cloudnet.ext.cloudperms.listener.PermissionsUpdateListener;
import de.dytanic.cloudnet.wrapper.Wrapper;
import lombok.Getter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Getter
public final class CloudPermissionsPermissionManagement implements IPermissionManagement {

    private static CloudPermissionsPermissionManagement instance;

    public static CloudPermissionsPermissionManagement getInstance() {
        return instance != null
            ? instance
            : (instance = new CloudPermissionsPermissionManagement());
    }

    private final Map<String, IPermissionGroup> cachedPermissionGroups = Maps.newConcurrentHashMap();

    private final Map<UUID, IPermissionUser> cachedPermissionUsers = Maps.newConcurrentHashMap();

    public CloudPermissionsPermissionManagement()
    {
        instance = this;

        init();
    }

    private void init()
    {
        for (IPermissionGroup permissionGroup : getDriver().getGroups())
            cachedPermissionGroups.put(permissionGroup.getName(), permissionGroup);

        getDriver().getEventManager().registerListener(new PermissionsUpdateListener());
    }

    public boolean hasPlayerPermission(IPermissionUser permissionUser, String perm)
    {
        Permission permission = new Permission(perm, 0);

        for (String group : Wrapper.getInstance().getServiceConfiguration().getGroups())
            if (hasPermission(permissionUser, group, permission))
                return true;

        return hasPermission(permissionUser, permission);
    }

    /*= ---------------------------------------------------------------------------------- =*/

    @Override
    public IPermissionManagementHandler getPermissionManagementHandler()
    {
        throw new UnsupportedOperationException("PermissionManagementHandler is not on this implementation");
    }

    @Override
    public void setPermissionManagementHandler(IPermissionManagementHandler permissionManagementHandler)
    {
        throw new UnsupportedOperationException("PermissionManagementHandler is not on this implementation");
    }

    @Override
    public IPermissionUser addUser(IPermissionUser permissionUser)
    {
        Validate.checkNotNull(permissionUser);

        try
        {
            getDriver().addUserAsync(permissionUser).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e)
        {
            e.printStackTrace();
        }

        return permissionUser;
    }

    @Override
    public void updateUser(IPermissionUser permissionUser)
    {
        Validate.checkNotNull(permissionUser);

        getDriver().updateUser(permissionUser);
    }

    @Override
    public void deleteUser(String name)
    {
        Validate.checkNotNull(name);

        getDriver().deleteUser(name);
    }

    @Override
    public void deleteUser(IPermissionUser permissionUser)
    {
        Validate.checkNotNull(permissionUser);

        getDriver().deleteUser(permissionUser);
    }

    @Override
    public boolean containsUser(UUID uniqueId)
    {
        Validate.checkNotNull(uniqueId);

        return cachedPermissionUsers.containsKey(uniqueId) || getDriver().containsUser(uniqueId);
    }

    @Override
    public boolean containsUser(String name)
    {
        Validate.checkNotNull(name);

        return getDriver().containsUser(name);
    }

    @Override
    public IPermissionUser getUser(UUID uniqueId)
    {
        Validate.checkNotNull(uniqueId);

        return cachedPermissionUsers.containsKey(uniqueId) ? cachedPermissionUsers.get(uniqueId) : getDriver().getUser(uniqueId);
    }

    @Override
    public List<IPermissionUser> getUser(String name)
    {
        Validate.checkNotNull(name);

        return getDriver().getUser(name);
    }

    @Override
    public Collection<IPermissionUser> getUsers()
    {
        return getDriver().getUsers();
    }

    @Override
    public void setUsers(Collection<? extends IPermissionUser> users)
    {
        Validate.checkNotNull(users);

        getDriver().setUsers(users);
    }

    @Override
    public Collection<IPermissionUser> getUserByGroup(String group)
    {
        Validate.checkNotNull(group);

        return getDriver().getUserByGroup(group);
    }

    @Override
    public IPermissionGroup addGroup(IPermissionGroup permissionGroup)
    {
        Validate.checkNotNull(permissionGroup);

        getDriver().addGroup(permissionGroup);

        return permissionGroup;
    }

    @Override
    public void updateGroup(IPermissionGroup permissionGroup)
    {
        Validate.checkNotNull(permissionGroup);

        getDriver().updateGroup(permissionGroup);
    }

    @Override
    public void deleteGroup(String group)
    {
        Validate.checkNotNull(group);

        getDriver().deleteGroup(group);
    }

    @Override
    public void deleteGroup(IPermissionGroup group)
    {
        Validate.checkNotNull(group);

        getDriver().deleteGroup(group);
    }

    @Override
    public boolean containsGroup(String name)
    {
        Validate.checkNotNull(name);

        return cachedPermissionGroups.containsKey(name);
    }

    @Override
    public IPermissionGroup getGroup(String name)
    {
        Validate.checkNotNull(name);

        return cachedPermissionGroups.get(name);
    }

    @Override
    public Collection<IPermissionGroup> getGroups()
    {
        return cachedPermissionGroups.values();
    }

    @Override
    public void setGroups(Collection<? extends IPermissionGroup> groups)
    {
        Validate.checkNotNull(groups);

        getDriver().setGroups(groups);
    }

    @Override
    public boolean reload()
    {
        Collection<IPermissionGroup> permissionGroups = getDriver().getGroups();

        cachedPermissionGroups.clear();

        for (IPermissionGroup group : permissionGroups)
            cachedPermissionGroups.put(group.getName(), group);

        return true;
    }

    /*= -------------------------------------------------------------- =*/

    private CloudNetDriver getDriver()
    {
        return CloudNetDriver.getInstance();
    }
}