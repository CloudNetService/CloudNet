package de.dytanic.cloudnet.ext.cloudperms.gomint;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.cloudperms.gomint.listener.GoMintCloudNetCloudPermissionsPlayerListener;
import de.dytanic.cloudnet.wrapper.Wrapper;
import io.gomint.GoMint;
import io.gomint.entity.EntityPlayer;
import io.gomint.plugin.Plugin;
import io.gomint.plugin.PluginName;
import io.gomint.plugin.Version;

@PluginName("CloudNet-CloudPerms")
@Version(major = 1, minor = 2)
public final class GoMintCloudNetCloudPermissionsPlugin extends Plugin {

    private static GoMintCloudNetCloudPermissionsPlugin instance;

    public static GoMintCloudNetCloudPermissionsPlugin getInstance() {
        return GoMintCloudNetCloudPermissionsPlugin.instance;
    }

    private CachedPermissionManagement permissionsManagement;

    @Override
    public void onStartup() {
        instance = this;
    }

    @Override
    public void onInstall() {
        this.permissionsManagement = CloudPermissionsManagement.newInstance();

        this.injectEntityPlayersCloudPermissionManager();

        super.registerListener(new GoMintCloudNetCloudPermissionsPlayerListener(this.permissionsManagement));
    }

    @Override
    public void onUninstall() {
        CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
        Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());
    }

    private void injectEntityPlayersCloudPermissionManager() {
        GoMint.instance().onlinePlayers().forEach(this::injectPermissionManager);
    }

    public void injectPermissionManager(EntityPlayer entityPlayer) {
        entityPlayer.permissionManager(new GoMintCloudNetCloudPermissionsPermissionManager(entityPlayer, this.permissionsManagement));
    }

}
