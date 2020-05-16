package de.dytanic.cloudnet.ext.cloudperms.nukkit.listener;

import cn.nukkit.Server;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerLoginEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsHelper;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsManagement;
import de.dytanic.cloudnet.ext.cloudperms.nukkit.NukkitCloudNetCloudPermissionsPlugin;

public final class NukkitCloudNetCloudPermissionsPlayerListener implements Listener {

    private final NukkitCloudNetCloudPermissionsPlugin plugin;

    private final CloudPermissionsManagement permissionsManagement;

    public NukkitCloudNetCloudPermissionsPlayerListener(NukkitCloudNetCloudPermissionsPlugin plugin, CloudPermissionsManagement permissionsManagement) {
        this.permissionsManagement = permissionsManagement;
        this.plugin = plugin;
    }

    @EventHandler
    public void handle(PlayerLoginEvent event) {
        CloudPermissionsHelper.initPermissionUser(this.permissionsManagement, event.getPlayer().getUniqueId(), event.getPlayer().getName(), message -> {
            event.setCancelled();
            event.setKickMessage(message.replace("&", "§"));
        }, Server.getInstance().getPropertyBoolean("xbox-auth", true));

        this.plugin.injectCloudPermissible(event.getPlayer());
    }

    @EventHandler
    public void handle(PlayerQuitEvent event) {
        this.permissionsManagement.getCachedPermissionUsers().remove(event.getPlayer().getUniqueId());
    }
}