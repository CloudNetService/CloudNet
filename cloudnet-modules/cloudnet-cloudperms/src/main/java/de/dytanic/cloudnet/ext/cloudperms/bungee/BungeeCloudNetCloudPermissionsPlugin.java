package de.dytanic.cloudnet.ext.cloudperms.bungee;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsManagement;
import de.dytanic.cloudnet.ext.cloudperms.bungee.listener.BungeeCloudNetCloudPermissionsPlayerListener;
import de.dytanic.cloudnet.wrapper.Wrapper;
import net.md_5.bungee.api.plugin.Plugin;

public final class BungeeCloudNetCloudPermissionsPlugin extends Plugin {

    private static BungeeCloudNetCloudPermissionsPlugin instance;

    public static BungeeCloudNetCloudPermissionsPlugin getInstance() {
        return BungeeCloudNetCloudPermissionsPlugin.instance;
    }

    private CloudPermissionsManagement permissionsManagement = CloudPermissionsManagement.newInstance();

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        getProxy().getPluginManager().registerListener(this, new BungeeCloudNetCloudPermissionsPlayerListener(this.permissionsManagement));
    }

    @Override
    public void onDisable() {
        CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
        Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());
    }
}