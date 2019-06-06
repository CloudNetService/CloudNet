package de.dytanic.cloudnet.event.permission;

import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;

public final class PermissionServiceSetEvent extends DriverEvent {

    private final IPermissionManagement permissionManager;

    public PermissionServiceSetEvent(IPermissionManagement permissionManager) {
        this.permissionManager = permissionManager;
    }

    public IPermissionManagement getPermissionManager() {
        return this.permissionManager;
    }
}