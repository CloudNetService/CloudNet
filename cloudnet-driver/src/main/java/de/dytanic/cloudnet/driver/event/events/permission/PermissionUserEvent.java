package de.dytanic.cloudnet.driver.event.events.permission;

import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import lombok.Getter;

@Getter
abstract class PermissionUserEvent extends PermissionEvent {

    private final IPermissionUser permissionUser;

    public PermissionUserEvent(IPermissionManagement permissionManagement, IPermissionUser permissionUser)
    {
        super(permissionManagement);

        this.permissionUser = permissionUser;
    }
}