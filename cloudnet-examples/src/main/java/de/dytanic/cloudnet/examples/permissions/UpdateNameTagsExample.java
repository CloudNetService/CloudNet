package de.dytanic.cloudnet.examples.permissions;

import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.EventPriority;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionUpdateUserEvent;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsPermissionManagement;
import de.dytanic.cloudnet.ext.cloudperms.bukkit.BukkitCloudNetCloudPermissionsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Function;

public final class UpdateNameTagsExample {

    private final Collection<UUID> nickedPlayers = Iterables.newArrayList();

    @EventHandler
    public void executeBukkitExampleOnPlayerJoinEvent(PlayerJoinEvent event)
    {
        BukkitCloudNetCloudPermissionsPlugin.getInstance().updateNameTags(event.getPlayer());
        //Sets the nametags and don't overwrite the scoreboard rather the scoreboard will updated
    }

    @EventListener(priority = EventPriority.LOWEST)
    public void handle(PermissionUpdateUserEvent event) //Live update of permission users
    {
        Player player = Bukkit.getPlayer(event.getPermissionUser().getName());

        if (player != null) BukkitCloudNetCloudPermissionsPlugin.getInstance().updateNameTags(player);
    }

    //For developers with a NickAPI or something like this
    public void nickExample(Player player)
    {
        BukkitCloudNetCloudPermissionsPlugin.getInstance().updateNameTags(player, new Function<Player, IPermissionGroup>() {
            @Override
            public IPermissionGroup apply(Player player)
            {
                if (isNicked(player))
                {
                    return CloudPermissionsPermissionManagement.getInstance().getCachedPermissionGroups().get("Default");
                }

                IPermissionUser permissionUser = CloudPermissionsPermissionManagement.getInstance().getUser(player.getUniqueId());

                return permissionUser == null ? null : CloudPermissionsPermissionManagement.getInstance().getHighestPermissionGroup(permissionUser);
            }
        });
    }

    public boolean isNicked(Player player)
    {
        return nickedPlayers.contains(player.getUniqueId());
    }
}