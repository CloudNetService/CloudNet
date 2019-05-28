package de.dytanic.cloudnet.ext.cloudperms.bungee.listener;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsPermissionManagement;
import java.lang.reflect.Method;
import java.util.UUID;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PermissionCheckEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public final class BungeeCloudNetCloudPermissionsPlayerListener implements
  Listener {

  @EventHandler
  public void handle(LoginEvent event) {
    UUID uniqueId = getUniqueId(event.getConnection().getClass(),
      event.getConnection());

    if (uniqueId != null) {
      IPermissionUser permissionUser = CloudPermissionsPermissionManagement
        .getInstance().getUser(uniqueId);

      if (permissionUser == null) {
        CloudPermissionsPermissionManagement.getInstance()
          .addUser(new PermissionUser(
            uniqueId,
            event.getConnection().getName(),
            null,
            0
          ));

        permissionUser = CloudPermissionsPermissionManagement.getInstance()
          .getUser(uniqueId);
      }

      if (permissionUser != null) {
        CloudPermissionsPermissionManagement.getInstance()
          .getCachedPermissionUsers()
          .put(permissionUser.getUniqueId(), permissionUser);

        permissionUser.setName(event.getConnection().getName());
        CloudPermissionsPermissionManagement.getInstance()
          .updateUser(permissionUser);
      }
    }
  }

  @EventHandler
  public void handle(PermissionCheckEvent event) {
    UUID uniqueId = getUniqueId(event.getSender().getClass(),
      event.getSender());

    if (uniqueId != null) {
      IPermissionUser permissionUser = CloudPermissionsPermissionManagement
        .getInstance().getUser(uniqueId);

      if (permissionUser != null) {
        event.setHasPermission(
          CloudPermissionsPermissionManagement.getInstance()
            .hasPlayerPermission(permissionUser, event.getPermission()));
      }
    }
  }

  @EventHandler
  public void handle(PlayerDisconnectEvent event) {
    UUID uniqueId = getUniqueId(event.getPlayer().getClass(),
      event.getPlayer());

    if (uniqueId != null) {
      CloudPermissionsPermissionManagement.getInstance()
        .getCachedPermissionUsers().remove(uniqueId);
    }
  }

  /*= --------------------------------------------------------- =*/

  private UUID getUniqueId(Class<?> clazz, Object instance) {
    Validate.checkNotNull(clazz);
    Validate.checkNotNull(instance);

    UUID uniqueId = null;

    try {
      Method method = clazz.getMethod("getUniqueId");
      method.setAccessible(true);
      uniqueId = (UUID) method.invoke(instance);

    } catch (Exception ignored) {
    }

    return uniqueId;
  }
}