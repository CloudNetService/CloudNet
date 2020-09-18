package de.dytanic.cloudnet.ext.cloudperms.gomint.listener;

import de.dytanic.cloudnet.driver.permission.CachedPermissionManagement;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsHelper;
import de.dytanic.cloudnet.ext.cloudperms.gomint.GoMintCloudNetCloudPermissionsPlugin;
import io.gomint.event.EventHandler;
import io.gomint.event.EventListener;
import io.gomint.event.player.PlayerLoginEvent;
import io.gomint.event.player.PlayerQuitEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public final class GoMintCloudNetCloudPermissionsPlayerListener implements EventListener {

    private final CachedPermissionManagement permissionsManagement;

    public GoMintCloudNetCloudPermissionsPlayerListener(CachedPermissionManagement permissionsManagement) {
        this.permissionsManagement = permissionsManagement;
    }

    @EventHandler
    public void handle(PlayerLoginEvent event) {
        CloudPermissionsHelper.initPermissionUser(this.permissionsManagement, event.getPlayer().getUUID(), event.getPlayer().getName(), message -> {
            event.setCancelled(true);
            event.setKickMessage(ChatColor.translateAlternateColorCodes('&', message));
        }, Bukkit.getOnlineMode());

        GoMintCloudNetCloudPermissionsPlugin.getInstance().injectPermissionManager(event.getPlayer());
    }

    @EventHandler
    public void handle(PlayerQuitEvent event) {
        this.permissionsManagement.getCachedPermissionUsers().remove(event.getPlayer().getUUID());
    }
}