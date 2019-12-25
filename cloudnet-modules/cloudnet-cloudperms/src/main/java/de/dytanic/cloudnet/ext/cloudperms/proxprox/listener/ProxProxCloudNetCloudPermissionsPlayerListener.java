package de.dytanic.cloudnet.ext.cloudperms.proxprox.listener;

import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsHelper;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsManagement;
import io.gomint.proxprox.api.event.PermissionCheckEvent;
import io.gomint.proxprox.api.event.PlayerLoginEvent;
import io.gomint.proxprox.api.event.PlayerQuitEvent;
import io.gomint.proxprox.api.plugin.event.EventHandler;
import io.gomint.proxprox.api.plugin.event.Listener;

public final class ProxProxCloudNetCloudPermissionsPlayerListener implements Listener {

    @EventHandler(priority = -64)
    public void handle(PlayerLoginEvent event) {
        CloudPermissionsHelper.initPermissionUser(event.getPlayer().getUUID(), event.getPlayer().getName());
    }

    @EventHandler
    public void handle(PermissionCheckEvent event) {
        IPermissionUser permissionUser = CloudPermissionsManagement.getInstance().getUser(event.getPlayer().getUUID());

        if (permissionUser != null) {
            event.setResult(CloudPermissionsManagement.getInstance().hasPlayerPermission(permissionUser, event.getPermission()));
        }
    }

    @EventHandler
    public void handle(PlayerQuitEvent event) {
        CloudPermissionsManagement.getInstance().getCachedPermissionUsers().remove(event.getPlayer().getUUID());
    }
}