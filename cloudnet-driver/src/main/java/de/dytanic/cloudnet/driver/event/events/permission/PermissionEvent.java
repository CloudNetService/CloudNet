package de.dytanic.cloudnet.driver.event.events.permission;

import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;

public abstract class PermissionEvent extends Event {

    private final IPermissionManagement permissionManagement;

    public PermissionEvent(IPermissionManagement permissionManagement) {
        this.permissionManagement = permissionManagement;
    }

    public IPermissionManagement getPermissionManagement() {
        return this.permissionManagement;
    }
}