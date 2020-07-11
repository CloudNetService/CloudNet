package de.dytanic.cloudnet.ext.syncproxy;

public final class SyncProxyConstants {

    public static final String
            SYNC_PROXY_CHANNEL_NAME = "sync_bungee_channel",
            SYNC_PROXY_UPDATE_CONFIGURATION = "update_sync_bungee_configuration",
            SYNC_PROXY_CHANNEL_GET_CONFIGURATION = "sync_bungee_get_sync_bungee_configuration";

    public static boolean CLOUD_PERMS_ENABLED;

    static {
        try {
            Class.forName("de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsManagement");
            CLOUD_PERMS_ENABLED = true;
        } catch (ClassNotFoundException ignored) {
            CLOUD_PERMS_ENABLED = false;
        }
    }

    public SyncProxyConstants() {
        throw new UnsupportedOperationException();
    }

}