package de.dytanic.cloudnet.ext.cloudperms.bungee.listener;

import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsHelper;
import java.util.UUID;
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

public final class BungeeCloudNetCloudPermissionsPlayerListener implements Listener {

  private final IPermissionManagement permissionsManagement;

  public BungeeCloudNetCloudPermissionsPlayerListener(IPermissionManagement permissionsManagement) {
    this.permissionsManagement = permissionsManagement;
  }

  @EventHandler(priority = EventPriority.LOW)
  public void handle(LoginEvent event) {
    if (!event.isCancelled()) {
      CloudPermissionsHelper.initPermissionUser(this.permissionsManagement, event.getConnection().getUniqueId(),
        event.getConnection().getName(), message -> {
          event.setCancelled(true);
          event.setCancelReason(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', message)));
        });
    }
  }

  @EventHandler
  public void handle(PermissionCheckEvent event) {
    CommandSender sender = event.getSender();
    if (sender instanceof ProxiedPlayer) {
      UUID uniqueId = ((ProxiedPlayer) sender).getUniqueId(); // must not be set ¯\_(ツ)_/¯
      if (uniqueId != null) {
        IPermissionUser permissionUser = this.permissionsManagement.getUser(uniqueId);
        if (permissionUser != null) {
          event.setHasPermission(this.permissionsManagement.hasPermission(permissionUser, event.getPermission()));
        }
      }
    }
  }

  @EventHandler
  public void handle(PlayerDisconnectEvent event) {
    CloudPermissionsHelper.handlePlayerQuit(this.permissionsManagement, event.getPlayer().getUniqueId());
  }
}
