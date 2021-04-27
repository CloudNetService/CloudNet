package de.dytanic.cloudnet.ext.cloudperms.bukkit.listener;

import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsHelper;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsManagement;
import de.dytanic.cloudnet.ext.cloudperms.bukkit.BukkitCloudNetCloudPermissionsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class BukkitCloudNetCloudPermissionsPlayerListener implements Listener {

    private final BukkitCloudNetCloudPermissionsPlugin plugin;
    private final CloudPermissionsManagement permissionsManagement;

    public BukkitCloudNetCloudPermissionsPlayerListener(BukkitCloudNetCloudPermissionsPlugin plugin, CloudPermissionsManagement permissionsManagement) {
        this.plugin = plugin;
        this.permissionsManagement = permissionsManagement;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void handle(PlayerLoginEvent event) {
        CloudPermissionsHelper.initPermissionUser(this.permissionsManagement, event.getPlayer().getUniqueId(), event.getPlayer().getName(), message -> {
            event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            event.setKickMessage(ChatColor.translateAlternateColorCodes('&', message));
        }, Bukkit.getOnlineMode());

        plugin.injectCloudPermissible(event.getPlayer());
    }

    @EventHandler
    public void handle(PlayerQuitEvent event) {
        this.permissionsManagement.getCachedPermissionUsers().remove(event.getPlayer().getUniqueId());
    }
}