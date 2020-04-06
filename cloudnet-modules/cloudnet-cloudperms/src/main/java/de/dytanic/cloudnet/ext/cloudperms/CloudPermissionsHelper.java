package de.dytanic.cloudnet.ext.cloudperms;

import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.driver.permission.PermissionUser;

import java.util.UUID;

public class CloudPermissionsHelper {

    private CloudPermissionsHelper() {
        throw new UnsupportedOperationException();
    }

    public static void initPermissionUser(CloudPermissionsManagement permissionsManagement, UUID uniqueId, String name) {
        initPermissionUser(permissionsManagement, uniqueId, name, true);
    }

    public static void initPermissionUser(CloudPermissionsManagement permissionsManagement, UUID uniqueId, String name, boolean shouldUpdateName) {
        IPermissionUser permissionUser = permissionsManagement.getUser(uniqueId);

        if (permissionUser == null) {
            permissionsManagement.addUser(new PermissionUser(
                    uniqueId,
                    name,
                    null,
                    0
            ));

            permissionUser = permissionsManagement.getUser(uniqueId);
        }

        if (permissionUser != null) {
            permissionsManagement.getCachedPermissionUsers().put(permissionUser.getUniqueId(), permissionUser);
            if (shouldUpdateName) {
                permissionUser.setName(name);
                permissionsManagement.updateUser(permissionUser);
            }
        }
    }

}
