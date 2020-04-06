package de.dytanic.cloudnet.ext.simplenametags.listener;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionUpdateGroupEvent;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionUpdateUserEvent;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsManagement;
import de.dytanic.cloudnet.ext.cloudperms.bukkit.BukkitCloudNetCloudPermissionsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class CloudNetSimpleNameTagsListener implements Listener {

    private final JavaPlugin plugin;

    public CloudNetSimpleNameTagsListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void handle(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(this.plugin, () -> BukkitCloudNetCloudPermissionsPlugin.getInstance().updateNameTags(event.getPlayer()));
    }

    @EventListener
    public void handle(PermissionUpdateUserEvent event) {
        Bukkit.getScheduler().runTask(this.plugin, () -> Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.getUniqueId().equals(event.getPermissionUser().getUniqueId()))
                .findFirst()
                .ifPresent(value -> BukkitCloudNetCloudPermissionsPlugin.getInstance().updateNameTags(value)));
    }

    @EventListener
    public void handle(PermissionUpdateGroupEvent event) {
        Bukkit.getScheduler().runTask(this.plugin, () -> Bukkit.getOnlinePlayers().forEach(player -> {
            IPermissionUser permissionUser = CloudPermissionsManagement.getInstance().getUser(player.getUniqueId());

            if (permissionUser != null && permissionUser.inGroup(event.getPermissionGroup().getName())) {
                BukkitCloudNetCloudPermissionsPlugin.getInstance().updateNameTags(player);
            }
        }));
    }

}