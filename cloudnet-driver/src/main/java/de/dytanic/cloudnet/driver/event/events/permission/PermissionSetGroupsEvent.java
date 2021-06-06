package de.dytanic.cloudnet.driver.event.events.permission;

import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import java.util.Collection;

public final class PermissionSetGroupsEvent extends PermissionEvent {

  private final Collection<? extends IPermissionGroup> groups;

  public PermissionSetGroupsEvent(IPermissionManagement permissionManagement,
    Collection<? extends IPermissionGroup> groups) {
    super(permissionManagement);

    this.groups = groups;
  }

  public Collection<? extends IPermissionGroup> getGroups() {
    return this.groups;
  }
}
