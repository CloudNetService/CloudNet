package de.dytanic.cloudnet.ext.bridge.gomint.listener;

import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;
import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.OnlyProxyProtection;
import de.dytanic.cloudnet.ext.bridge.gomint.GoMintCloudNetHelper;
import de.dytanic.cloudnet.wrapper.Wrapper;
import io.gomint.ChatColor;
import io.gomint.event.EventHandler;
import io.gomint.event.EventListener;
import io.gomint.event.player.PlayerJoinEvent;
import io.gomint.event.player.PlayerLoginEvent;
import io.gomint.event.player.PlayerQuitEvent;
import io.gomint.plugin.Plugin;
import io.gomint.server.GoMintServer;
import io.gomint.server.entity.EntityPlayer;

public final class GoMintPlayerListener implements EventListener {

    private final Plugin plugin;

    private final OnlyProxyProtection onlyProxyProtection;

    public GoMintPlayerListener(Plugin plugin) {
        this.plugin = plugin;

        this.onlyProxyProtection = new OnlyProxyProtection(((GoMintServer) plugin.getServer()).getEncryptionKeyFactory().isKeyGiven());
    }

    @EventHandler
    public void handle(PlayerLoginEvent event) {
        EntityPlayer player = (EntityPlayer) event.getPlayer();
        BridgeConfiguration bridgeConfiguration = BridgeConfigurationProvider.load();

        if (this.onlyProxyProtection.shouldDisallowPlayer(player.getConnection().getConnection().getAddress().getAddress().getHostAddress())) {
            event.setCancelled(true);
            event.setKickMessage(ChatColor.translateAlternateColorCodes('&', bridgeConfiguration.getMessages().get("server-join-cancel-because-only-proxy")));
            return;
        }

        String currentTaskName = Wrapper.getInstance().getServiceId().getTaskName();
        ServiceTask serviceTask = Wrapper.getInstance().getServiceTaskProvider().getServiceTask(currentTaskName);

        if (serviceTask != null) {
            String requiredPermission = serviceTask.getProperties().getString("requiredPermission");
            if (requiredPermission != null && !player.hasPermission(requiredPermission)) {
                event.setCancelled(true);
                event.setKickMessage(ChatColor.translateAlternateColorCodes('&', bridgeConfiguration.getMessages().get("server-join-cancel-because-permission")));
                return;
            }

            if (serviceTask.isMaintenance() && !player.hasPermission("cloudnet.bridge.maintenance")) {
                event.setCancelled(true);
                event.setKickMessage(ChatColor.translateAlternateColorCodes('&', bridgeConfiguration.getMessages().get("server-join-cancel-because-maintenance")));
                return;
            }
        }

        BridgeHelper.sendChannelMessageServerLoginRequest(GoMintCloudNetHelper.createNetworkConnectionInfo(event.getPlayer()),
                GoMintCloudNetHelper.createNetworkPlayerServerInfo(event.getPlayer(), true));
    }

    @EventHandler
    public void handle(PlayerJoinEvent event) {
        BridgeHelper.sendChannelMessageServerLoginSuccess(GoMintCloudNetHelper.createNetworkConnectionInfo(event.getPlayer()),
                GoMintCloudNetHelper.createNetworkPlayerServerInfo(event.getPlayer(), false));

        BridgeHelper.updateServiceInfo();
    }

    @EventHandler
    public void handle(PlayerQuitEvent event) {
        BridgeHelper.sendChannelMessageServerDisconnect(GoMintCloudNetHelper.createNetworkConnectionInfo(event.getPlayer()),
                GoMintCloudNetHelper.createNetworkPlayerServerInfo(event.getPlayer(), false));

        this.plugin.getScheduler().execute(BridgeHelper::updateServiceInfo);
    }
}