package de.dytanic.cloudnet.ext.cloudperms.bukkit.listener;

import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsPermissionManagement;
import de.dytanic.cloudnet.ext.cloudperms.bukkit.BukkitCloudNetCloudPermissionsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class BukkitCloudNetCloudPermissionsPlayerListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void handle(PlayerLoginEvent event)
    {
        IPermissionUser permissionUser = CloudPermissionsPermissionManagement.getInstance().getUser(event.getPlayer().getUniqueId());

        if (permissionUser == null)
        {
            CloudPermissionsPermissionManagement.getInstance().addUser(new PermissionUser(
                event.getPlayer().getUniqueId(),
                event.getPlayer().getName(),
                null,
                0
            ));

            permissionUser = CloudPermissionsPermissionManagement.getInstance().getUser(event.getPlayer().getUniqueId());
        }

        if (permissionUser != null)
        {
            CloudPermissionsPermissionManagement.getInstance().getCachedPermissionUsers().put(permissionUser.getUniqueId(), permissionUser);

            if (Bukkit.getOnlineMode())
            {
                permissionUser.setName(event.getPlayer().getName());
                CloudPermissionsPermissionManagement.getInstance().updateUser(permissionUser);
            }
        }

        BukkitCloudNetCloudPermissionsPlugin.getInstance().injectCloudPermissible(event.getPlayer());
    }

    @EventHandler
    public void handle(PlayerQuitEvent event)
    {
        CloudPermissionsPermissionManagement.getInstance().getCachedPermissionUsers().remove(event.getPlayer().getUniqueId());
    }
}