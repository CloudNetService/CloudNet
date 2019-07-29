package de.dytanic.cloudnet.ext.simplenametags.listener;

import de.dytanic.cloudnet.common.Value;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionUpdateGroupEvent;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionUpdateUserEvent;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsPermissionManagement;
import de.dytanic.cloudnet.ext.cloudperms.bukkit.BukkitCloudNetCloudPermissionsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.lang.reflect.Method;
import java.util.function.Predicate;

public final class CloudNetSimpleNameTagsListener implements Listener {

    @EventHandler
    public void handle(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(Bukkit.getPluginManager().getPlugin("CloudNet-SimpleNameTags"), () -> BukkitCloudNetCloudPermissionsPlugin.getInstance().updateNameTags(event.getPlayer()), 4L);
    }

    @EventListener
    public void handle(PermissionUpdateUserEvent event) {
        Value<Player> player = new Value<>();

        forEachPlayers(p -> {
            if (p.getUniqueId().equals(event.getPermissionUser().getUniqueId())) {
                player.setValue(p);
                return true;
            }

            return false;
        });

        if (player.getValue() != null) {
            BukkitCloudNetCloudPermissionsPlugin.getInstance().updateNameTags(player.getValue());
        }
    }

    @EventListener
    public void handle(PermissionUpdateGroupEvent event) {
        forEachPlayers(player -> {
            IPermissionUser permissionUser = CloudPermissionsPermissionManagement.getInstance().getUser(player.getUniqueId());

            if (permissionUser != null && permissionUser.inGroup(event.getPermissionGroup().getName())) {
                BukkitCloudNetCloudPermissionsPlugin.getInstance().updateNameTags(player);
            }

            return false;
        });
    }


    private void forEachPlayers(Predicate<Player> predicate) {
        Method method;
        try {
            method = Server.class.getMethod("getOnlinePlayers");
            method.setAccessible(true);
            Object result = method.invoke(Bukkit.getServer());

            if (result instanceof Iterable) {
                for (Object item : ((Iterable) result)) {
                    if (predicate.test((Player) item)) {
                        return;
                    }
                }
            }

            if (result instanceof Player[]) {
                for (Player player : ((Player[]) result)) {
                    if (predicate.test(player)) {
                        return;
                    }
                }
            }

        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}