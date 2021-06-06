package de.dytanic.cloudnet.ext.bridge.gomint.listener;

import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;
import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.OnlyProxyProtection;
import de.dytanic.cloudnet.ext.bridge.gomint.GoMintCloudNetHelper;
import de.dytanic.cloudnet.wrapper.Wrapper;
import io.gomint.ChatColor;
import io.gomint.entity.EntityPlayer;
import io.gomint.event.EventHandler;
import io.gomint.event.EventListener;
import io.gomint.event.player.PlayerJoinEvent;
import io.gomint.event.player.PlayerLoginEvent;
import io.gomint.event.player.PlayerQuitEvent;
import io.gomint.plugin.Plugin;
import io.gomint.server.GoMintServer;

public final class GoMintPlayerListener implements EventListener {

  private final Plugin plugin;

  private final OnlyProxyProtection onlyProxyProtection;

  public GoMintPlayerListener(Plugin plugin) {
    this.plugin = plugin;

    this.onlyProxyProtection = new OnlyProxyProtection(
      ((GoMintServer) plugin.server()).encryptionKeyFactory().isKeyGiven());
  }

  @EventHandler
  public void handle(PlayerLoginEvent event) {
    EntityPlayer player = event.player();
    BridgeConfiguration bridgeConfiguration = BridgeConfigurationProvider.load();

    if (this.onlyProxyProtection.shouldDisallowPlayer(player.address().getAddress().getHostAddress())) {
      event.cancelled(true);
      event.kickMessage(ChatColor.translateAlternateColorCodes('&',
        bridgeConfiguration.getMessages().get("server-join-cancel-because-only-proxy")));
      return;
    }

    String currentTaskName = Wrapper.getInstance().getServiceId().getTaskName();
    ServiceTask serviceTask = Wrapper.getInstance().getServiceTaskProvider().getServiceTask(currentTaskName);

    if (serviceTask != null) {
      String requiredPermission = serviceTask.getProperties().getString("requiredPermission");
      if (requiredPermission != null && !player.permissionManager().has(requiredPermission)) {
        event.cancelled(true);
        event.kickMessage(ChatColor.translateAlternateColorCodes('&',
          bridgeConfiguration.getMessages().get("server-join-cancel-because-permission")));
        return;
      }

      if (serviceTask.isMaintenance() && !player.permissionManager().has("cloudnet.bridge.maintenance")) {
        event.cancelled(true);
        event.kickMessage(ChatColor.translateAlternateColorCodes('&',
          bridgeConfiguration.getMessages().get("server-join-cancel-because-maintenance")));
        return;
      }
    }

    BridgeHelper.sendChannelMessageServerLoginRequest(GoMintCloudNetHelper.createNetworkConnectionInfo(player),
      GoMintCloudNetHelper.createNetworkPlayerServerInfo(player, true));
  }

  @EventHandler
  public void handle(PlayerJoinEvent event) {
    BridgeHelper.sendChannelMessageServerLoginSuccess(GoMintCloudNetHelper.createNetworkConnectionInfo(event.player()),
      GoMintCloudNetHelper.createNetworkPlayerServerInfo(event.player(), false));

    BridgeHelper.updateServiceInfo();
  }

  @EventHandler
  public void handle(PlayerQuitEvent event) {
    BridgeHelper.sendChannelMessageServerDisconnect(GoMintCloudNetHelper.createNetworkConnectionInfo(event.player()),
      GoMintCloudNetHelper.createNetworkPlayerServerInfo(event.player(), false));

    this.plugin.scheduler().execute(BridgeHelper::updateServiceInfo);
  }

}
