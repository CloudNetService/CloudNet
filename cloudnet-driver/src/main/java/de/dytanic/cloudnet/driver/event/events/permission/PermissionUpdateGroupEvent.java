package de.dytanic.cloudnet.driver.event.events.permission;

import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;

public final class PermissionUpdateGroupEvent extends PermissionGroupEvent {

    public PermissionUpdateGroupEvent(IPermissionManagement permissionManagement, IPermissionGroup permissionGroup) {
        super(permissionManagement, permissionGroup);
    }
}