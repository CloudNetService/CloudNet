package de.dytanic.cloudnet.ext.cloudperms;

import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.driver.permission.PermissionUser;

import java.util.UUID;

public class CloudPermissionsHelper {

    private CloudPermissionsHelper() {
        throw new UnsupportedOperationException();
    }

    public static void initPermissionUser(UUID uniqueId, String name) {
        initPermissionUser(uniqueId, name, true);
    }

    public static void initPermissionUser(UUID uniqueId, String name, boolean shouldUpdateName) {
        IPermissionUser permissionUser = CloudPermissionsManagement.getInstance().getUser(uniqueId);

        if (permissionUser == null) {
            CloudPermissionsManagement.getInstance().addUser(new PermissionUser(
                    uniqueId,
                    name,
                    null,
                    0
            ));

            permissionUser = CloudPermissionsManagement.getInstance().getUser(uniqueId);
        }

        if (permissionUser != null) {
            CloudPermissionsManagement.getInstance().getCachedPermissionUsers().put(permissionUser.getUniqueId(), permissionUser);
            if (shouldUpdateName) {
                permissionUser.setName(name);
                CloudPermissionsManagement.getInstance().updateUser(permissionUser);
            }
        }
    }

}
