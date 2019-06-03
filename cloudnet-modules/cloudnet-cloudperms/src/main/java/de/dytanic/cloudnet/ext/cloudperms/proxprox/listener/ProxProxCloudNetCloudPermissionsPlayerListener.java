package de.dytanic.cloudnet.ext.cloudperms.proxprox.listener;

import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsPermissionManagement;
import io.gomint.proxprox.api.event.PermissionCheckEvent;
import io.gomint.proxprox.api.event.PlayerLoginEvent;
import io.gomint.proxprox.api.event.PlayerQuitEvent;
import io.gomint.proxprox.api.plugin.event.EventHandler;
import io.gomint.proxprox.api.plugin.event.Listener;

public final class ProxProxCloudNetCloudPermissionsPlayerListener implements Listener {

    @EventHandler(priority = -64)
    public void handle(PlayerLoginEvent event)
    {
        IPermissionUser permissionUser = CloudPermissionsPermissionManagement.getInstance().getUser(event.getPlayer().getUUID());

        if (permissionUser == null)
        {
            CloudPermissionsPermissionManagement.getInstance().addUser(new PermissionUser(
                event.getPlayer().getUUID(),
                event.getPlayer().getName(),
                null,
                0
            ));

            permissionUser = CloudPermissionsPermissionManagement.getInstance().getUser(event.getPlayer().getUUID());
        }

        if (permissionUser != null)
        {
            CloudPermissionsPermissionManagement.getInstance().getCachedPermissionUsers().put(permissionUser.getUniqueId(), permissionUser);

            permissionUser.setName(event.getPlayer().getName());
            CloudPermissionsPermissionManagement.getInstance().updateUser(permissionUser);
        }
    }

    @EventHandler
    public void handle(PermissionCheckEvent event)
    {
        IPermissionUser permissionUser = CloudPermissionsPermissionManagement.getInstance().getUser(event.getPlayer().getUUID());

        if (permissionUser != null)
            event.setResult(CloudPermissionsPermissionManagement.getInstance().hasPlayerPermission(permissionUser, event.getPermission()));
    }

    @EventHandler
    public void handle(PlayerQuitEvent event)
    {
        CloudPermissionsPermissionManagement.getInstance().getCachedPermissionUsers().remove(event.getPlayer().getUUID());
    }
}