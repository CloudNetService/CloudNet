package de.dytanic.cloudnet.examples.permissions;

import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsPermissionManagement;
import java.util.concurrent.TimeUnit;
import org.bukkit.entity.Player;

public final class PlayerAddOrRemovePermissionAndGroups {

  public void addPermission(Player player) {
    IPermissionUser permissionUser = CloudPermissionsPermissionManagement
        .getInstance().getUser(player.getUniqueId());

    if (permissionUser == null) {
      return;
    }

    permissionUser
        .addPermission("minecraft.command.gamemode", true); //adds a permission
    permissionUser.addPermission("CityBuild",
        "minecraft.command.difficulty"); //adds a permission which the effect works only on CityBuild group services
    CloudPermissionsPermissionManagement.getInstance()
        .updateUser(permissionUser);
  }

  public void removePermission(Player player) {
    IPermissionUser permissionUser = CloudPermissionsPermissionManagement
        .getInstance().getUser(player.getUniqueId());

    permissionUser
        .removePermission("minecraft.command.gamemode"); //removes a permission
    permissionUser.removePermission("CityBuild",
        "minecraft.command.difficulty"); //removes a group specific permission
    CloudPermissionsPermissionManagement.getInstance()
        .updateUser(permissionUser);
  }

  public void addGroup(Player player) {
    IPermissionUser permissionUser = CloudPermissionsPermissionManagement
        .getInstance().getUser(player.getUniqueId());

    permissionUser.addGroup("Admin"); //add Admin group
    permissionUser
        .addGroup("YouTuber", 5, TimeUnit.DAYS); //add YouTuber group for 5 days
    CloudPermissionsPermissionManagement.getInstance()
        .updateUser(permissionUser);

    if (permissionUser.inGroup("Admin")) {
      player.sendMessage("Your in group Admin!");
    }
  }

  public void removeGroup(Player player) {
    IPermissionUser permissionUser = CloudPermissionsPermissionManagement
        .getInstance().getUser(player.getUniqueId());

    permissionUser.removeGroup("YouTuber"); //removes the YouTuber group
    CloudPermissionsPermissionManagement.getInstance()
        .updateUser(permissionUser);
  }
}