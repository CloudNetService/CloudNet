package de.dytanic.cloudnet.examples.permissions;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

public final class PlayerAddOrRemovePermissionAndGroups {

    public void addPermission(Player player) {
        CloudNetDriver.getInstance().getPermissionManagement().modifyUser(player.getUniqueId(), permissionUser -> {
            permissionUser.addPermission("minecraft.command.gamemode", true); //adds a permission
            permissionUser.addPermission("CityBuild", "minecraft.command.difficulty"); //adds a permission which the effect works only on CityBuild group services
        });
    }

    public void removePermission(Player player) {
        CloudNetDriver.getInstance().getPermissionManagement().modifyUser(player.getUniqueId(), permissionUser -> {
            permissionUser.removePermission("minecraft.command.gamemode"); //removes a permission
            permissionUser.removePermission("CityBuild", "minecraft.command.difficulty"); //removes a group specific permission
        });
    }

    public void addGroup(Player player) {
        CloudNetDriver.getInstance().getPermissionManagement().modifyUser(player.getUniqueId(), permissionUser -> {
            permissionUser.addGroup("Admin"); //add Admin group
            permissionUser.addGroup("YouTuber", 5, TimeUnit.DAYS); //add YouTuber group for 5 days

            if (permissionUser.inGroup("Admin")) {
                player.sendMessage("Your in group Admin!");
            }
        });
    }

    public void removeGroup(Player player) {
        CloudNetDriver.getInstance().getPermissionManagement().modifyUser(player.getUniqueId(), permissionUser -> {
            permissionUser.removeGroup("YouTuber"); //removes the YouTuber group
        });
    }
}