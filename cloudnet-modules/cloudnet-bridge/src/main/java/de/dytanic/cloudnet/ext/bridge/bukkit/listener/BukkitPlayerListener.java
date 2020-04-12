package de.dytanic.cloudnet.ext.bridge.bukkit.listener;

import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;
import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.bukkit.BukkitCloudNetBridgePlugin;
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

import java.util.Arrays;

public final class BukkitPlayerListener implements Listener {

    private final BukkitCloudNetBridgePlugin plugin;

    private final BridgeConfiguration bridgeConfiguration;

    private final boolean onlyProxyProtection;

    public BukkitPlayerListener(BukkitCloudNetBridgePlugin plugin) {
        this.plugin = plugin;
        this.bridgeConfiguration = BridgeConfigurationProvider.load();
        this.onlyProxyProtection = !Bukkit.getOnlineMode()
                && this.bridgeConfiguration != null
                && this.bridgeConfiguration.isOnlyProxyProtection()
                && this.bridgeConfiguration.getExcludedOnlyProxyWalkableGroups() != null
                && this.bridgeConfiguration.getExcludedOnlyProxyWalkableGroups().stream()
                .noneMatch(group -> Arrays.asList(Wrapper.getInstance().getServiceConfiguration().getGroups()).contains(group));
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void handle(PlayerLoginEvent event) {
        Player player = event.getPlayer();

        String currentTaskName = Wrapper.getInstance().getServiceId().getTaskName();
        ServiceTask serviceTask = Wrapper.getInstance().getServiceTaskProvider().getServiceTask(currentTaskName);

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
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTask(this.plugin, () -> {
            BridgeHelper.sendChannelMessageServerDisconnect(BukkitCloudNetHelper.createNetworkConnectionInfo(player),
                    BukkitCloudNetHelper.createNetworkPlayerServerInfo(player, false));

            BridgeHelper.updateServiceInfo();
        });
    }

}