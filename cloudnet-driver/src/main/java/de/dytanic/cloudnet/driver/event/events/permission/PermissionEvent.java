package de.dytanic.cloudnet.driver.event.events.permission;

import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import org.jetbrains.annotations.NotNull;

public abstract class PermissionEvent extends Event {

  private final IPermissionManagement permissionManagement;

  public PermissionEvent(@NotNull IPermissionManagement permissionManagement) {
    this.permissionManagement = permissionManagement;
  }

  @NotNull
  public IPermissionManagement getPermissionManagement() {
    return this.permissionManagement;
  }
}
