package de.dytanic.cloudnet.ext.bridge.gomint.listener;

import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.gomint.GoMintCloudNetHelper;
import io.gomint.event.EventHandler;
import io.gomint.event.EventListener;
import io.gomint.event.player.PlayerJoinEvent;
import io.gomint.event.player.PlayerLoginEvent;
import io.gomint.event.player.PlayerQuitEvent;

public final class GoMintPlayerListener implements EventListener {

  @EventHandler
  public void handle(PlayerLoginEvent event) {
    BridgeHelper.sendChannelMessageServerLoginRequest(
        GoMintCloudNetHelper.createNetworkConnectionInfo(event.getPlayer()),
        GoMintCloudNetHelper
            .createNetworkPlayerServerInfo(event.getPlayer(), true));
  }

  @EventHandler
  public void handle(PlayerJoinEvent event) {
    BridgeHelper.sendChannelMessageServerLoginSuccess(
        GoMintCloudNetHelper.createNetworkConnectionInfo(event.getPlayer()),
        GoMintCloudNetHelper
            .createNetworkPlayerServerInfo(event.getPlayer(), false));

    BridgeHelper.updateServiceInfo();
  }

  @EventHandler
  public void handle(PlayerQuitEvent event) {
    BridgeHelper.sendChannelMessageServerDisconnect(
        GoMintCloudNetHelper.createNetworkConnectionInfo(event.getPlayer()),
        GoMintCloudNetHelper
            .createNetworkPlayerServerInfo(event.getPlayer(), false));

    GoMintCloudNetHelper.getPlugin().getScheduler().execute(new Runnable() {
      @Override
      public void run() {
        BridgeHelper.updateServiceInfo();
      }
    });
  }
}