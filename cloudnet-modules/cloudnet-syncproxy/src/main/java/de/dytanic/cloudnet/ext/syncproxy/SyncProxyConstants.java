package de.dytanic.cloudnet.ext.syncproxy;

import de.dytanic.cloudnet.driver.permission.IPermissionManagement;

import java.lang.reflect.InvocationTargetException;

public final class SyncProxyConstants {

    public static final String
            SYNC_PROXY_CHANNEL_NAME = "sync_bungee_channel",
            SYNC_PROXY_UPDATE_CONFIGURATION = "update_sync_bungee_configuration",
            SYNC_PROXY_SYNC_CHANNEL_PROPERTY = "cloudnet_sync_bungee_channel",
            SIGN_CHANNEL_SYNC_ID_GET_SYNC_PROXY_CONFIGURATION_PROPERTY = "sync_bungee_get_sync_bungee_configuration",
            SYNC_PROXY_SERVICE_INFO_SNAPSHOT_ONLINE_COUNT = "Online-Count";

    public static IPermissionManagement PERMISSION_MANAGEMENT;

    static {
        try {
            PERMISSION_MANAGEMENT = (IPermissionManagement) Class.forName("de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsPermissionManagement")
                    .getDeclaredMethod("getInstance")
                    .invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }
    }

    public SyncProxyConstants() {
        throw new UnsupportedOperationException();
    }

}