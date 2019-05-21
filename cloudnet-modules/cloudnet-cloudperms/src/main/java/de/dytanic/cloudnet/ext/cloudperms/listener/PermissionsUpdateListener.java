package de.dytanic.cloudnet.ext.cloudperms.listener;


import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.permission.*;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsPermissionManagement;

public final class PermissionsUpdateListener {

    /*
    @EventListener
    public void handle(PermissionAddUserEvent event)
    {
    }
    */

    @EventListener
    public void handle(PermissionUpdateUserEvent event)
    {
        if (CloudPermissionsPermissionManagement.getInstance().getCachedPermissionUsers().containsKey(event.getPermissionUser().getUniqueId()))
            CloudPermissionsPermissionManagement.getInstance().getCachedPermissionUsers().put(event.getPermissionUser().getUniqueId(), event.getPermissionUser());
    }

    @EventListener
    public void handle(PermissionDeleteUserEvent event)
    {
        CloudPermissionsPermissionManagement.getInstance().getCachedPermissionUsers().remove(event.getPermissionUser().getUniqueId());
    }

    /*
    @EventListener
    public void handle(PermissionSetUsersEvent event)
    {

    }
    */

    @EventListener
    public void handle(PermissionAddGroupEvent event)
    {
        CloudPermissionsPermissionManagement.getInstance().getCachedPermissionGroups().put(event.getPermissionGroup().getName(), event.getPermissionGroup());
    }

    @EventListener
    public void handle(PermissionUpdateGroupEvent event)
    {
        CloudPermissionsPermissionManagement.getInstance().getCachedPermissionGroups().put(event.getPermissionGroup().getName(), event.getPermissionGroup());
    }

    @EventListener
    public void handle(PermissionDeleteGroupEvent event)
    {
        CloudPermissionsPermissionManagement.getInstance().getCachedPermissionGroups().remove(event.getPermissionGroup().getName());
    }

    @EventListener
    public void handle(PermissionSetGroupsEvent event)
    {
        CloudPermissionsPermissionManagement.getInstance().getCachedPermissionGroups().clear();

        for (IPermissionGroup permissionGroup : event.getGroups())
            CloudPermissionsPermissionManagement.getInstance().getCachedPermissionGroups().put(permissionGroup.getName(), permissionGroup);
    }
}