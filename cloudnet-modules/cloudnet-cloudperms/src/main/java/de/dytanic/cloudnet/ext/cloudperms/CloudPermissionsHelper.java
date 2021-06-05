package de.dytanic.cloudnet.ext.cloudperms;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.permission.CachedPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public final class CloudPermissionsHelper {

    private CloudPermissionsHelper() {
        throw new UnsupportedOperationException();
    }

    public static void initPermissionUser(IPermissionManagement permissionsManagement, UUID uniqueId, String name, Consumer<String> disconnectHandler) {
        initPermissionUser(permissionsManagement, uniqueId, name, disconnectHandler, true);
    }

    public static void initPermissionUser(IPermissionManagement permissionsManagement, UUID uniqueId, String name, Consumer<String> disconnectHandler, boolean shouldUpdateName) {
        IPermissionUser permissionUser = null;
        try {
            permissionUser = permissionsManagement.getOrCreateUserAsync(uniqueId, name).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            CloudNetDriver.getInstance().getLogger().error("Error while loading permission user: " + uniqueId + "/" + name, exception);
        }

        if (permissionUser != null) {
            CachedPermissionManagement management = asCachedPermissionManagement(permissionsManagement);
            if (management != null) {
                management.acquireLock(permissionUser);
            }

            if (shouldUpdateName && !name.equals(permissionUser.getName())) {
                permissionUser.setName(name);
                permissionsManagement.updateUserAsync(permissionUser);
            }
        } else {
            disconnectHandler.accept("§cAn internal error occurred while loading the permissions"); // TODO configurable
        }
    }

    public static void handlePlayerQuit(IPermissionManagement permissionsManagement, UUID uniqueId) {
        CachedPermissionManagement management = asCachedPermissionManagement(permissionsManagement);
        if (management != null) {
            IPermissionUser cachedUser = management.getCachedUser(uniqueId);
            if (cachedUser != null) {
                management.unlock(cachedUser);
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
