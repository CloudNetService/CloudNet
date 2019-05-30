package de.dytanic.cloudnet.driver.event.events.permission;

import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;

public final class PermissionAddGroupEvent extends PermissionGroupEvent {

  public PermissionAddGroupEvent(IPermissionManagement permissionManagement,
    IPermissionGroup permissionGroup) {
    super(permissionManagement, permissionGroup);
  }
}