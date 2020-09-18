package de.dytanic.cloudnet.ext.cloudperms.gomint;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.permission.CachedPermissionManagement;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsManagement;
import de.dytanic.cloudnet.ext.cloudperms.gomint.listener.GoMintCloudNetCloudPermissionsPlayerListener;
import de.dytanic.cloudnet.wrapper.Wrapper;
import io.gomint.GoMint;
import io.gomint.entity.EntityPlayer;
import io.gomint.plugin.Plugin;
import io.gomint.plugin.PluginName;
import io.gomint.plugin.Version;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;

@PluginName("CloudNet-CloudPerms")
@Version(major = 1, minor = 0)
public final class GoMintCloudNetCloudPermissionsPlugin extends Plugin {

    private static GoMintCloudNetCloudPermissionsPlugin instance;
    private CachedPermissionManagement permissionsManagement;

    public static GoMintCloudNetCloudPermissionsPlugin getInstance() {
        return GoMintCloudNetCloudPermissionsPlugin.instance;
    }

    @Override
    public void onInstall() {
        instance = this;
    }

    @Override
    public void onStartup() {
        this.permissionsManagement = CloudPermissionsManagement.newInstance();
        injectEntityPlayersCloudPermissionManager();

        registerListener(new GoMintCloudNetCloudPermissionsPlayerListener(this.permissionsManagement));
    }

    @Override
    public void onUninstall() {
        CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
        Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());
    }


    private void injectEntityPlayersCloudPermissionManager() {
        for (EntityPlayer entityPlayer : GoMint.instance().getPlayers()) {
            injectPermissionManager(entityPlayer);
        }
    }

    public void injectPermissionManager(EntityPlayer entityPlayer) {
        try {
            Field field = entityPlayer.getClass().getDeclaredField("permissionManager");
            field.setAccessible(true);

            Field modifiersField = Field.class.getDeclaredField("modifiers");

            AccessController.doPrivileged((PrivilegedAction) () -> {
                modifiersField.setAccessible(true);
                return null;
            });

            modifiersField.setInt(field, modifiersField.getModifiers() & ~Modifier.FINAL);
            field.set(entityPlayer, new GoMintCloudNetCloudPermissionsPermissionManager((io.gomint.server.entity.EntityPlayer) entityPlayer, entityPlayer.getPermissionManager()));

        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

}