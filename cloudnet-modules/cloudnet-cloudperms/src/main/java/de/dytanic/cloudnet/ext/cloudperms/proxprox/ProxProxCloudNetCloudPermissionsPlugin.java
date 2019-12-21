package de.dytanic.cloudnet.ext.cloudperms.proxprox;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsManagement;
import de.dytanic.cloudnet.ext.cloudperms.proxprox.listener.ProxProxCloudNetCloudPermissionsPlayerListener;
import de.dytanic.cloudnet.wrapper.Wrapper;
import io.gomint.proxprox.api.plugin.Plugin;
import io.gomint.proxprox.api.plugin.annotation.Description;
import io.gomint.proxprox.api.plugin.annotation.Name;
import io.gomint.proxprox.api.plugin.annotation.Version;

@Name("CloudNet-CloudPerms")
@Version(major = 1, minor = 0)
@Description("ProxProx extension which implement the permission management system from CloudNet into ProxProx for players")
public final class ProxProxCloudNetCloudPermissionsPlugin extends Plugin {

    @Override
    public void onStartup() {
        new CloudPermissionsManagement();

        registerListener(new ProxProxCloudNetCloudPermissionsPlayerListener());
    }

    @Override
    public void onUninstall() {
        CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
        Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());
    }
}