package de.dytanic.cloudnet.examples.permissions;

import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.EventPriority;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionUpdateUserEvent;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsManagement;
import de.dytanic.cloudnet.ext.cloudperms.bukkit.BukkitCloudNetCloudPermissionsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Collection;
import java.util.UUID;

public final class UpdateNameTagsExample {

    private final Collection<UUID> nickedPlayers = Iterables.newArrayList();

    @EventHandler
    public void executeBukkitExampleOnPlayerJoinEvent(PlayerJoinEvent event) {
        BukkitCloudNetCloudPermissionsPlugin.getInstance().updateNameTags(event.getPlayer());
        //Sets the nametags and don't overwrite the scoreboard rather the scoreboard will updated
    }

    @EventListener(priority = EventPriority.LOWEST)
    public void handle(PermissionUpdateUserEvent event) //Live update of permission users
    {
        Player player = Bukkit.getPlayer(event.getPermissionUser().getUniqueId());

        if (player != null) {
            BukkitCloudNetCloudPermissionsPlugin.getInstance().updateNameTags(player);
        }
    }

    //For developers with a NickAPI or something like this
    public void nickExample(Player player) {
        BukkitCloudNetCloudPermissionsPlugin.getInstance().updateNameTags(player, player1 -> {
            if (isNicked(player1)) {
                return CloudPermissionsManagement.getInstance().getCachedPermissionGroups().get("Default");
            }

            IPermissionUser permissionUser = CloudPermissionsManagement.getInstance().getUser(player1.getUniqueId());

            return permissionUser == null ? null : CloudPermissionsManagement.getInstance().getHighestPermissionGroup(permissionUser);
        });
    }

    public boolean isNicked(Player player) {
        return nickedPlayers.contains(player.getUniqueId());
    }
}