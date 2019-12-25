package de.dytanic.cloudnet.ext.cloudperms.listener;


import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.permission.*;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsManagement;

public final class PermissionsUpdateListener {

    /*
    @EventListener
    public void handle(PermissionAddUserEvent event)
    {
    }
    */

    @EventListener
    public void handle(PermissionUpdateUserEvent event) {
        if (CloudPermissionsManagement.getInstance().getCachedPermissionUsers().containsKey(event.getPermissionUser().getUniqueId())) {
            CloudPermissionsManagement.getInstance().getCachedPermissionUsers().put(event.getPermissionUser().getUniqueId(), event.getPermissionUser());
        }
    }

    @EventListener
    public void handle(PermissionDeleteUserEvent event) {
        CloudPermissionsManagement.getInstance().getCachedPermissionUsers().remove(event.getPermissionUser().getUniqueId());
    }

    /*
    @EventListener
    public void handle(PermissionSetUsersEvent event)
    {

    }
    */

    @EventListener
    public void handle(PermissionAddGroupEvent event) {
        CloudPermissionsManagement.getInstance().getCachedPermissionGroups().put(event.getPermissionGroup().getName(), event.getPermissionGroup());
    }

    @EventListener
    public void handle(PermissionUpdateGroupEvent event) {
        CloudPermissionsManagement.getInstance().getCachedPermissionGroups().put(event.getPermissionGroup().getName(), event.getPermissionGroup());
    }

    @EventListener
    public void handle(PermissionDeleteGroupEvent event) {
        CloudPermissionsManagement.getInstance().getCachedPermissionGroups().remove(event.getPermissionGroup().getName());
    }

    @EventListener
    public void handle(PermissionSetGroupsEvent event) {
        CloudPermissionsManagement.getInstance().getCachedPermissionGroups().clear();

        for (IPermissionGroup permissionGroup : event.getGroups()) {
            CloudPermissionsManagement.getInstance().getCachedPermissionGroups().put(permissionGroup.getName(), permissionGroup);
        }
    }
}