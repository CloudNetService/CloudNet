package de.dytanic.cloudnet.driver.event.events.permission;

import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import lombok.Getter;

import java.util.Collection;

@Getter
public final class PermissionSetUsersEvent extends PermissionEvent {

    private final Collection<? extends IPermissionUser> users;

    public PermissionSetUsersEvent(IPermissionManagement permissionManagement, Collection<? extends IPermissionUser> users) {
        super(permissionManagement);

        this.users = users;
    }
}