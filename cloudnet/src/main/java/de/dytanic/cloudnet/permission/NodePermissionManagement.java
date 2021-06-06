package de.dytanic.cloudnet.permission;

import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionManagementHandler;

public interface NodePermissionManagement extends IPermissionManagement {

  IPermissionManagementHandler getPermissionManagementHandler();

  void setPermissionManagementHandler(IPermissionManagementHandler permissionManagementHandler);

}
