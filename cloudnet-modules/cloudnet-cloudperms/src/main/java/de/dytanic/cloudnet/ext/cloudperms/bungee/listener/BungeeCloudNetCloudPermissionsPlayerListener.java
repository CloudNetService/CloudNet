package de.dytanic.cloudnet.ext.cloudperms.bungee.listener;

import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsHelper;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsManagement;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PermissionCheckEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

public final class BungeeCloudNetCloudPermissionsPlayerListener implements Listener {

    private CloudPermissionsManagement permissionsManagement;

    public BungeeCloudNetCloudPermissionsPlayerListener(CloudPermissionsManagement permissionsManagement) {
        this.permissionsManagement = permissionsManagement;
    }

    @EventHandler
    public void handle(LoginEvent event) {
        CloudPermissionsHelper.initPermissionUser(this.permissionsManagement, event.getConnection().getUniqueId(), event.getConnection().getName());
    }

    @EventHandler
    public void handle(PermissionCheckEvent event) {
        CommandSender sender = event.getSender();

        UUID uniqueId = null;

        if (sender instanceof ProxiedPlayer) {
            uniqueId = ((ProxiedPlayer) sender).getUniqueId();
        } else {
            try {
                Method method = sender.getClass().getDeclaredMethod("getUniqueId");
                uniqueId = (UUID) method.invoke(sender);
            } catch (NoSuchMethodException ignored) {
            } catch (IllegalAccessException | InvocationTargetException exception) {
                exception.printStackTrace();
            }
        }

        if (uniqueId != null) {
            IPermissionUser permissionUser = this.permissionsManagement.getUser(uniqueId);

            if (permissionUser != null) {
                event.setHasPermission(this.permissionsManagement.hasPlayerPermission(permissionUser, event.getPermission()));
            }
        }
    }

    @EventHandler
    public void handle(PlayerDisconnectEvent event) {
        UUID uniqueId = event.getPlayer().getUniqueId();

        this.permissionsManagement.getCachedPermissionUsers().remove(uniqueId);

    }

}