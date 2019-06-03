package de.dytanic.cloudnet.driver.event.events.permission;

import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;

public final class PermissionDeleteUserEvent extends PermissionUserEvent {

    public PermissionDeleteUserEvent(IPermissionManagement permissionManagement, IPermissionUser permissionUser)
    {
        super(permissionManagement, permissionUser);
    }
}