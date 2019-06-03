package de.dytanic.cloudnet.event.permission;

import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class PermissionServiceSetEvent extends DriverEvent {

    private final IPermissionManagement permissionManager;

}