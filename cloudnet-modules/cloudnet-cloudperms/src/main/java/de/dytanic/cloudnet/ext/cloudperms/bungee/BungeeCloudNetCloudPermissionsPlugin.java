package de.dytanic.cloudnet.ext.cloudperms.bungee;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsPermissionManagement;
import de.dytanic.cloudnet.ext.cloudperms.bungee.listener.BungeeCloudNetCloudPermissionsPlayerListener;
import de.dytanic.cloudnet.wrapper.Wrapper;
import lombok.Getter;
import net.md_5.bungee.api.plugin.Plugin;

@Getter
public final class BungeeCloudNetCloudPermissionsPlugin extends Plugin {

    @Getter
    private static BungeeCloudNetCloudPermissionsPlugin instance;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        new CloudPermissionsPermissionManagement();

        getProxy().getPluginManager().registerListener(this, new BungeeCloudNetCloudPermissionsPlayerListener());
    }

    @Override
    public void onDisable() {
        CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
        Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());
    }
}