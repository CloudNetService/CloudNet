package de.dytanic.cloudnet.wrapper.permission;


import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.permission.*;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class PermissionCacheListener {

    private final WrapperPermissionManagement permissionManagement;

    public PermissionCacheListener(WrapperPermissionManagement permissionManagement) {
        this.permissionManagement = permissionManagement;
    }
/*
    @EventListener
    public void handle(PermissionAddUserEvent event)
    {
    }
    */

    @EventListener
    public void handle(PermissionUpdateUserEvent event) {
        if (this.permissionManagement.getCachedPermissionUsers().containsKey(event.getPermissionUser().getUniqueId())) {
            this.permissionManagement.getCachedPermissionUsers().put(event.getPermissionUser().getUniqueId(), event.getPermissionUser());
        }
    }

    @EventListener
    public void handle(PermissionDeleteUserEvent event) {
        this.permissionManagement.getCachedPermissionUsers().remove(event.getPermissionUser().getUniqueId());
    }

    /*
    @EventListener
    public void handle(PermissionSetUsersEvent event)
    {

    }
    */

    @EventListener
    public void handle(PermissionAddGroupEvent event) {
        this.permissionManagement.getCachedPermissionGroups().put(event.getPermissionGroup().getName(), event.getPermissionGroup());
    }

    @EventListener
    public void handle(PermissionUpdateGroupEvent event) {
        this.permissionManagement.getCachedPermissionGroups().put(event.getPermissionGroup().getName(), event.getPermissionGroup());
    }

    @EventListener
    public void handle(PermissionDeleteGroupEvent event) {
        this.permissionManagement.getCachedPermissionGroups().remove(event.getPermissionGroup().getName());
    }

    @EventListener
    public void handle(PermissionSetGroupsEvent event) {
        this.permissionManagement.getCachedPermissionGroups().clear();

        for (IPermissionGroup permissionGroup : event.getGroups()) {
            this.permissionManagement.getCachedPermissionGroups().put(permissionGroup.getName(), permissionGroup);
        }
    }
}