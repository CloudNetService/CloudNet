package de.dytanic.cloudnet.driver.permission;

import java.util.Collection;

public interface IPermissionManagementHandler {

  void handleAddUser(IPermissionManagement permissionManagement,
      IPermissionUser permissionUser);

  void handleUpdateUser(IPermissionManagement permissionManagement,
      IPermissionUser permissionUser);

  void handleDeleteUser(IPermissionManagement permissionManagement,
      IPermissionUser permissionUser);

  void handleSetUsers(IPermissionManagement permissionManagement,
      Collection<? extends IPermissionUser> users);

  void handleAddGroup(IPermissionManagement permissionManagement,
      IPermissionGroup permissionGroup);

  void handleUpdateGroup(IPermissionManagement permissionManagement,
      IPermissionGroup permissionGroup);

  void handleDeleteGroup(IPermissionManagement permissionManagement,
      IPermissionGroup permissionGroup);

  void handleSetGroups(IPermissionManagement permissionManagement,
      Collection<? extends IPermissionGroup> groups);

  void handleReloaded(IPermissionManagement permissionManagement);
}