package de.dytanic.cloudnet.ext.cloudperms.gomint.listener;

import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsHelper;
import de.dytanic.cloudnet.ext.cloudperms.gomint.GoMintCloudNetCloudPermissionsPlugin;
import io.gomint.ChatColor;
import io.gomint.GoMint;
import io.gomint.entity.EntityPlayer;
import io.gomint.event.EventHandler;
import io.gomint.event.EventListener;
import io.gomint.event.EventPriority;
import io.gomint.event.player.PlayerLoginEvent;
import io.gomint.event.player.PlayerQuitEvent;
import io.gomint.server.GoMintServer;

public final class GoMintCloudNetCloudPermissionsPlayerListener implements EventListener {

  private final IPermissionManagement permissionsManagement;

  public GoMintCloudNetCloudPermissionsPlayerListener(IPermissionManagement permissionsManagement) {
    this.permissionsManagement = permissionsManagement;
  }

  @EventHandler(priority = EventPriority.LOW)
  public void handle(PlayerLoginEvent event) {
    if (!event.cancelled()) {
      EntityPlayer player = event.player();

      CloudPermissionsHelper.initPermissionUser(this.permissionsManagement, player.uuid(), player.name(), message -> {
        event.cancelled(true);
        event.kickMessage(ChatColor.translateAlternateColorCodes('&', message));
      }, ((GoMintServer) GoMint.instance()).encryptionKeyFactory().isKeyGiven());

      GoMintCloudNetCloudPermissionsPlugin.getInstance().injectPermissionManager(player);
    }
  }

  @EventHandler
  public void handle(PlayerQuitEvent event) {
    CloudPermissionsHelper.handlePlayerQuit(this.permissionsManagement, event.player().uuid());
  }
}
