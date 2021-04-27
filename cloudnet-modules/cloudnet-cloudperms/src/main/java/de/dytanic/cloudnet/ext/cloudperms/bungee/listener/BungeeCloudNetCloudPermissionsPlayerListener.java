package de.dytanic.cloudnet.ext.cloudperms.bungee.listener;

import de.dytanic.cloudnet.driver.permission.CachedPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsHelper;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PermissionCheckEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

public final class BungeeCloudNetCloudPermissionsPlayerListener implements Listener {

    private final IPermissionManagement permissionsManagement;

    public BungeeCloudNetCloudPermissionsPlayerListener(IPermissionManagement permissionsManagement) {
        this.permissionsManagement = permissionsManagement;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void handle(LoginEvent event) {
        if (!event.isCancelled()) {
            CloudPermissionsHelper.initPermissionUser(this.permissionsManagement, event.getConnection().getUniqueId(), event.getConnection().getName(), message -> {
                event.setCancelled(true);
                event.setCancelReason(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', message)));
            });
        }
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
                event.setHasPermission(this.permissionsManagement.hasPermission(permissionUser, event.getPermission()));
            }
        }
    }

    @EventHandler
    public void handle(PlayerDisconnectEvent event) {
        CachedPermissionManagement management = CloudPermissionsHelper.asCachedPermissionManagement(this.permissionsManagement);
        if (management != null) {
            management.getCachedPermissionUsers().remove(event.getPlayer().getUniqueId());
        }
    }
}
