package de.dytanic.cloudnet.ext.cloudperms.gomint.listener;

import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsPermissionManagement;
import de.dytanic.cloudnet.ext.cloudperms.gomint.GoMintCloudNetCloudPermissionsPlugin;
import io.gomint.event.EventHandler;
import io.gomint.event.EventListener;
import io.gomint.event.player.PlayerLoginEvent;
import io.gomint.event.player.PlayerQuitEvent;

public final class GoMintCloudNetCloudPermissionsPlayerListener implements EventListener {

    @EventHandler
    public void handle(PlayerLoginEvent event) {
        IPermissionUser permissionUser = CloudPermissionsPermissionManagement.getInstance().getUser(event.getPlayer().getUUID());

        if (permissionUser == null) {
            CloudPermissionsPermissionManagement.getInstance().addUser(new PermissionUser(
                    event.getPlayer().getUUID(),
                    event.getPlayer().getName(),
                    null,
                    0
            ));

            permissionUser = CloudPermissionsPermissionManagement.getInstance().getUser(event.getPlayer().getUUID());
        }

        if (permissionUser != null) {
            CloudPermissionsPermissionManagement.getInstance().getCachedPermissionUsers().put(permissionUser.getUniqueId(), permissionUser);
            permissionUser.setName(event.getPlayer().getName());
            CloudPermissionsPermissionManagement.getInstance().updateUser(permissionUser);
        }

        GoMintCloudNetCloudPermissionsPlugin.getInstance().injectPermissionManager(event.getPlayer());
    }

    @EventHandler
    public void handle(PlayerQuitEvent event) {
        CloudPermissionsPermissionManagement.getInstance().getCachedPermissionUsers().remove(event.getPlayer().getUUID());
    }
}