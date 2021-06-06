package de.dytanic.cloudnet.driver.event.events.permission;

import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;

abstract class PermissionGroupEvent extends PermissionEvent {

  private final IPermissionGroup permissionGroup;

  public PermissionGroupEvent(IPermissionManagement permissionManagement, IPermissionGroup permissionGroup) {
    super(permissionManagement);

    this.permissionGroup = permissionGroup;
  }

  public IPermissionGroup getPermissionGroup() {
    return this.permissionGroup;
  }
}
