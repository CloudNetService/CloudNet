package de.dytanic.cloudnet.ext.cloudperms.gomint.listener;

import de.dytanic.cloudnet.driver.permission.CachedPermissionManagement;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsHelper;
import de.dytanic.cloudnet.ext.cloudperms.gomint.GoMintCloudNetCloudPermissionsPlugin;
import io.gomint.ChatColor;
import io.gomint.GoMint;
import io.gomint.entity.EntityPlayer;
import io.gomint.event.EventHandler;
import io.gomint.event.EventListener;
import io.gomint.event.EventPriority;
import io.gomint.event.player.PlayerLoginEvent;
import io.gomint.event.player.PlayerQuitEvent;
import io.gomint.server.GoMintServer;

public final class GoMintCloudNetCloudPermissionsPlayerListener implements EventListener {

    private final CachedPermissionManagement permissionsManagement;

    public GoMintCloudNetCloudPermissionsPlayerListener(CachedPermissionManagement permissionsManagement) {
        this.permissionsManagement = permissionsManagement;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void handle(PlayerLoginEvent event) {
        if (!event.isCancelled()) {
            EntityPlayer player = event.getPlayer();

            CloudPermissionsHelper.initPermissionUser(this.permissionsManagement, player.getUUID(), event.getPlayer().getName(), message -> {
                event.setCancelled(true);
                event.setKickMessage(ChatColor.translateAlternateColorCodes('&', message));
            }, ((GoMintServer) GoMint.instance()).getEncryptionKeyFactory().isKeyGiven());

            GoMintCloudNetCloudPermissionsPlugin.getInstance().injectPermissionManager(player);
        }
    }

    @EventHandler
    public void handle(PlayerQuitEvent event) {
        this.permissionsManagement.getCachedPermissionUsers().remove(event.getPlayer().getUUID());
    }

}