package de.dytanic.cloudnet.driver.event.events.permission;

import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public abstract class PermissionEvent extends Event {

    private final IPermissionManagement permissionManagement;

}