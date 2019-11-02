package de.dytanic.cloudnet.ext.cloudperms.gomint.listener;

import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsHelper;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsPermissionManagement;
import de.dytanic.cloudnet.ext.cloudperms.gomint.GoMintCloudNetCloudPermissionsPlugin;
import io.gomint.event.EventHandler;
import io.gomint.event.EventListener;
import io.gomint.event.player.PlayerLoginEvent;
import io.gomint.event.player.PlayerQuitEvent;

public final class GoMintCloudNetCloudPermissionsPlayerListener implements EventListener {

    @EventHandler
    public void handle(PlayerLoginEvent event) {
        CloudPermissionsHelper.initPermissionUser(event.getPlayer().getUUID(), event.getPlayer().getName());

        GoMintCloudNetCloudPermissionsPlugin.getInstance().injectPermissionManager(event.getPlayer());
    }

    @EventHandler
    public void handle(PlayerQuitEvent event) {
        CloudPermissionsPermissionManagement.getInstance().getCachedPermissionUsers().remove(event.getPlayer().getUUID());
    }
}