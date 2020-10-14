package de.dytanic.cloudnet.driver.permission;

import java.util.Map;
import java.util.UUID;

public interface CachedPermissionManagement extends IPermissionManagement {

    Map<UUID, IPermissionUser> getCachedPermissionUsers();

    Map<String, IPermissionGroup> getCachedPermissionGroups();

}
