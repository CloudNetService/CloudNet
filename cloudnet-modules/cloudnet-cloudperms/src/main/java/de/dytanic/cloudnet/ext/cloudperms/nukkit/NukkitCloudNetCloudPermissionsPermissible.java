package de.dytanic.cloudnet.ext.cloudperms.nukkit;

import cn.nukkit.Player;
import cn.nukkit.permission.PermissibleBase;
import cn.nukkit.permission.Permission;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsPermissionManagement;
import lombok.Getter;

@Getter
public final class NukkitCloudNetCloudPermissionsPermissible extends
    PermissibleBase {

  private final Player player;

  public NukkitCloudNetCloudPermissionsPermissible(Player player) {
    super(player);

    this.player = player;
  }

  @Override
  public boolean isPermissionSet(String name) {
    return hasPermission(name);
  }

  @Override
  public boolean isPermissionSet(Permission perm) {
    return isPermissionSet(perm.getName());
  }

  @Override
  public boolean hasPermission(Permission perm) {
    return hasPermission(perm.getName());
  }

  @Override
  public boolean hasPermission(String inName) {
    if (inName == null) {
      return false;
    }

    IPermissionUser permissionUser = CloudPermissionsPermissionManagement
        .getInstance().getUser(player.getUniqueId());
    return permissionUser != null && CloudPermissionsPermissionManagement
        .getInstance().hasPlayerPermission(permissionUser, inName);
  }
}