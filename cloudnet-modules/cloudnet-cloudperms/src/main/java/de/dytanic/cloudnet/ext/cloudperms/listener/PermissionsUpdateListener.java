package de.dytanic.cloudnet.ext.cloudperms.listener;


import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.permission.*;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsManagement;

public final class PermissionsUpdateListener {

    private CloudPermissionsManagement permissionsManagement;

    public PermissionsUpdateListener(CloudPermissionsManagement permissionsManagement) {
        this.permissionsManagement = permissionsManagement;
    }
/*
    @EventListener
    public void handle(PermissionAddUserEvent event)
    {
    }
    */

    @EventListener
    public void handle(PermissionUpdateUserEvent event) {
        if (this.permissionsManagement.getCachedPermissionUsers().containsKey(event.getPermissionUser().getUniqueId())) {
            this.permissionsManagement.getCachedPermissionUsers().put(event.getPermissionUser().getUniqueId(), event.getPermissionUser());
        }
    }

    @EventListener
    public void handle(PermissionDeleteUserEvent event) {
        this.permissionsManagement.getCachedPermissionUsers().remove(event.getPermissionUser().getUniqueId());
    }

    /*
    @EventListener
    public void handle(PermissionSetUsersEvent event)
    {

    }
    */

    @EventListener
    public void handle(PermissionAddGroupEvent event) {
        this.permissionsManagement.getCachedPermissionGroups().put(event.getPermissionGroup().getName(), event.getPermissionGroup());
    }

    @EventListener
    public void handle(PermissionUpdateGroupEvent event) {
        this.permissionsManagement.getCachedPermissionGroups().put(event.getPermissionGroup().getName(), event.getPermissionGroup());
    }

    @EventListener
    public void handle(PermissionDeleteGroupEvent event) {
        this.permissionsManagement.getCachedPermissionGroups().remove(event.getPermissionGroup().getName());
    }

    @EventListener
    public void handle(PermissionSetGroupsEvent event) {
        this.permissionsManagement.getCachedPermissionGroups().clear();

        for (IPermissionGroup permissionGroup : event.getGroups()) {
            this.permissionsManagement.getCachedPermissionGroups().put(permissionGroup.getName(), permissionGroup);
        }
    }
}