package de.dytanic.cloudnet.ext.bridge.bukkit.listener;

import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;
import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.bukkit.BukkitCloudNetHelper;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class BukkitPlayerListener implements Listener {

    private final BridgeConfiguration bridgeConfiguration;

    private final boolean onlyProxyProtection;

    public BukkitPlayerListener() {
        this.bridgeConfiguration = BridgeConfigurationProvider.load();
        this.onlyProxyProtection = !Bukkit.getOnlineMode()
                && this.bridgeConfiguration != null
                && this.bridgeConfiguration.isOnlyProxyProtection()
                && this.bridgeConfiguration.getExcludedOnlyProxyWalkableGroups() != null
                && this.bridgeConfiguration.getExcludedOnlyProxyWalkableGroups().stream()
                .noneMatch(group -> Iterables.contains(group, Wrapper.getInstance().getServiceConfiguration().getGroups()));
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void handle(PlayerLoginEvent event) {
        Player player = event.getPlayer();

        String currentTaskName = Wrapper.getInstance().getServiceId().getTaskName();
        ServiceTask serviceTask = Wrapper.getInstance().getServiceTask(currentTaskName);

        if (serviceTask != null && serviceTask.isMaintenance() && !player.hasPermission("cloudnet.bridge.maintenance")) {
            event.setResult(PlayerLoginEvent.Result.KICK_WHITELIST);
            event.setKickMessage(ChatColor.translateAlternateColorCodes('&', this.bridgeConfiguration.getMessages().get("server-join-cancel-because-maintenance")));
            return;
        }

        if (this.onlyProxyProtection && !BridgeHelper.playerIsOnProxy(player.getUniqueId(), event.getRealAddress().getHostAddress())) {
            event.setResult(PlayerLoginEvent.Result.KICK_WHITELIST);
            event.setKickMessage(ChatColor.translateAlternateColorCodes('&', this.bridgeConfiguration.getMessages().get("server-join-cancel-because-only-proxy")));
            return;
        }

        BridgeHelper.sendChannelMessageServerLoginRequest(BukkitCloudNetHelper.createNetworkConnectionInfo(player),
                BukkitCloudNetHelper.createNetworkPlayerServerInfo(player, true)
        );
    }

    @EventHandler
    public void handle(PlayerJoinEvent event) {
        BridgeHelper.sendChannelMessageServerLoginSuccess(BukkitCloudNetHelper.createNetworkConnectionInfo(event.getPlayer()),
                BukkitCloudNetHelper.createNetworkPlayerServerInfo(event.getPlayer(), false));

        BridgeHelper.updateServiceInfo();
    }

    @EventHandler
    public void handle(PlayerQuitEvent event) {
        BridgeHelper.sendChannelMessageServerDisconnect(BukkitCloudNetHelper.createNetworkConnectionInfo(event.getPlayer()),
                BukkitCloudNetHelper.createNetworkPlayerServerInfo(event.getPlayer(), false));

        Wrapper.getInstance().runTask(BridgeHelper::updateServiceInfo);
    }

}