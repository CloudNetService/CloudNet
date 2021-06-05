package de.dytanic.cloudnet.ext.cloudperms.nukkit.listener;

import cn.nukkit.Server;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerAsyncPreLoginEvent;
import cn.nukkit.event.player.PlayerLoginEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsHelper;
import de.dytanic.cloudnet.ext.cloudperms.nukkit.NukkitCloudNetCloudPermissionsPlugin;

public final class NukkitCloudNetCloudPermissionsPlayerListener implements Listener {

    private final NukkitCloudNetCloudPermissionsPlugin plugin;
    private final IPermissionManagement permissionsManagement;

    public NukkitCloudNetCloudPermissionsPlayerListener(NukkitCloudNetCloudPermissionsPlugin plugin, IPermissionManagement permissionsManagement) {
        this.plugin = plugin;
        this.permissionsManagement = permissionsManagement;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void handle(PlayerAsyncPreLoginEvent event) {
        if (!event.isCancelled()) {
            CloudPermissionsHelper.initPermissionUser(this.permissionsManagement, event.getPlayer().getUniqueId(), event.getPlayer().getName(), message -> {
                event.setCancelled();
                event.setKickMessage(message.replace("&", "§"));
            }, Server.getInstance().getPropertyBoolean("xbox-auth", true));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void handle(PlayerLoginEvent event) {
        if (!event.isCancelled()) {
            this.plugin.injectCloudPermissible(event.getPlayer());
        }
    }

    @EventHandler
    public void handle(PlayerQuitEvent event) {
        CloudPermissionsHelper.handlePlayerQuit(this.permissionsManagement, event.getPlayer().getUniqueId());
    }
}
