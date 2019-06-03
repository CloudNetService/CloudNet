package de.dytanic.cloudnet.ext.cloudperms.nukkit.listener;

import cn.nukkit.Server;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerLoginEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsPermissionManagement;
import de.dytanic.cloudnet.ext.cloudperms.nukkit.NukkitCloudNetCloudPermissionsPlugin;

public final class NukkitCloudNetCloudPermissionsPlayerListener implements Listener {

    @EventHandler
    public void handle(PlayerLoginEvent event) {
        IPermissionUser permissionUser = CloudPermissionsPermissionManagement.getInstance().getUser(event.getPlayer().getUniqueId());

        if (permissionUser == null) {
            CloudPermissionsPermissionManagement.getInstance().addUser(new PermissionUser(
                    event.getPlayer().getUniqueId(),
                    event.getPlayer().getName(),
                    null,
                    0
            ));

            permissionUser = CloudPermissionsPermissionManagement.getInstance().getUser(event.getPlayer().getUniqueId());
        }

        if (permissionUser != null) {
            CloudPermissionsPermissionManagement.getInstance().getCachedPermissionUsers().put(permissionUser.getUniqueId(), permissionUser);

            if (Server.getInstance().getPropertyBoolean("xbox-auth", true)) {
                permissionUser.setName(event.getPlayer().getName());
                CloudPermissionsPermissionManagement.getInstance().updateUser(permissionUser);
            }
        }

        NukkitCloudNetCloudPermissionsPlugin.getInstance().injectCloudPermissible(event.getPlayer());
    }

    @EventHandler
    public void handle(PlayerQuitEvent event) {
        CloudPermissionsPermissionManagement.getInstance().getCachedPermissionUsers().remove(event.getPlayer().getUniqueId());
    }
}