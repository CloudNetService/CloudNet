package de.dytanic.cloudnet.ext.cloudperms;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.permission.CachedPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.driver.permission.PermissionUser;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class CloudPermissionsHelper {

    private CloudPermissionsHelper() {
        throw new UnsupportedOperationException();
    }

    public static void initPermissionUser(IPermissionManagement permissionsManagement, UUID uniqueId, String name, Consumer<String> disconnectHandler) {
        initPermissionUser(permissionsManagement, uniqueId, name, disconnectHandler, true);
    }

    public static void initPermissionUser(IPermissionManagement permissionsManagement, UUID uniqueId, String name, Consumer<String> disconnectHandler, boolean shouldUpdateName) {
        IPermissionUser permissionUser = null;
        try {
            permissionUser = permissionsManagement.getUserAsync(uniqueId).get(3, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException exception) {
            exception.printStackTrace();
        } catch (TimeoutException exception) {
            disconnectHandler.accept("§cAn internal error occurred while loading the permissions"); // TODO configurable
            return;
        }

        if (permissionUser == null) {
            permissionsManagement.addUser(new PermissionUser(
                    uniqueId,
                    name,
                    null,
                    0
            ));

            permissionUser = permissionsManagement.getUser(uniqueId);
            shouldUpdateName = false;
        }

        if (permissionUser != null) {
            CachedPermissionManagement management = asCachedPermissionManagement(permissionsManagement);
            if (management != null) {
                management.getCachedPermissionUsers().put(uniqueId, permissionUser);
            }

            if (shouldUpdateName && !name.equals(permissionUser.getName())) {
                permissionUser.setName(name);
                permissionsManagement.updateUser(permissionUser);
            }
        }
    }

    public static CachedPermissionManagement getCachedPermissionManagement() {
        return asCachedPermissionManagement(CloudNetDriver.getInstance().getPermissionManagement());
    }

    public static CachedPermissionManagement asCachedPermissionManagement(IPermissionManagement management) {
        return management instanceof CachedPermissionManagement ? (CachedPermissionManagement) management : null;
    }
}
