package de.dytanic.cloudnet.ext.cloudperms.bukkit.listener;

import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsHelper;
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
    public void handle(PlayerLoginEvent event) {
        CloudPermissionsHelper.initPermissionUser(event.getPlayer().getUniqueId(), event.getPlayer().getName(), Bukkit.getOnlineMode());

        BukkitCloudNetCloudPermissionsPlugin.getInstance().injectCloudPermissible(event.getPlayer());
    }

    @EventHandler
    public void handle(PlayerQuitEvent event) {
        CloudPermissionsPermissionManagement.getInstance().getCachedPermissionUsers().remove(event.getPlayer().getUniqueId());
    }
}