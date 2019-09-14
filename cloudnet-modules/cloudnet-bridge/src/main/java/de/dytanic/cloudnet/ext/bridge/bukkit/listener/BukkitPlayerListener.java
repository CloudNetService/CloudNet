package de.dytanic.cloudnet.ext.bridge.bukkit.listener;

import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;
import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.bukkit.BukkitCloudNetBridgePlugin;
import de.dytanic.cloudnet.ext.bridge.bukkit.BukkitCloudNetHelper;
import de.dytanic.cloudnet.ext.bridge.bukkit.event.BukkitBridgeProxyPlayerServerConnectRequestEvent;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collection;
import java.util.UUID;

public final class BukkitPlayerListener implements Listener {

    private final BukkitCloudNetBridgePlugin plugin;

    private final BridgeConfiguration bridgeConfiguration = BridgeConfigurationProvider.load();

    private final Collection<UUID> accessUniqueIds = Iterables.newCopyOnWriteArrayList();

    public BukkitPlayerListener(BukkitCloudNetBridgePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void handle(BukkitBridgeProxyPlayerServerConnectRequestEvent event) {
        if (Bukkit.getOnlineMode()) {
            return;
        }

        if (this.bridgeConfiguration != null && this.bridgeConfiguration.getExcludedOnlyProxyWalkableGroups() != null) {
            if (this.bridgeConfiguration.getExcludedOnlyProxyWalkableGroups().stream()
                    .anyMatch(group -> Iterables.contains(group, Wrapper.getInstance().getServiceConfiguration().getGroups()))) {
                return;
            }

        }

        if (event.getNetworkConnectionInfo().getUniqueId() != null) {

            UUID uniqueId = event.getNetworkConnectionInfo().getUniqueId();

            this.accessUniqueIds.add(uniqueId);
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.accessUniqueIds.remove(uniqueId), 40);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void handle(PlayerLoginEvent event) {
        boolean onlyProxyProtection = !Bukkit.getOnlineMode();

        if (onlyProxyProtection && this.bridgeConfiguration != null && this.bridgeConfiguration.getExcludedOnlyProxyWalkableGroups() != null) {
            onlyProxyProtection = this.bridgeConfiguration.getExcludedOnlyProxyWalkableGroups().stream()
                    .noneMatch(group -> Iterables.contains(group, Wrapper.getInstance().getServiceConfiguration().getGroups()));
        }

        if (onlyProxyProtection) {
            UUID uniqueId = event.getPlayer().getUniqueId();

            if (!this.accessUniqueIds.contains(uniqueId)) {
                event.setResult(PlayerLoginEvent.Result.KICK_WHITELIST);
                event.setKickMessage(ChatColor.translateAlternateColorCodes('&', this.bridgeConfiguration.getMessages().get("server-join-cancel-because-only-proxy")));
                return;

            } else {
                this.accessUniqueIds.remove(uniqueId);
            }

        }

        BridgeHelper.sendChannelMessageServerLoginRequest(BukkitCloudNetHelper.createNetworkConnectionInfo(event.getPlayer()),
                BukkitCloudNetHelper.createNetworkPlayerServerInfo(event.getPlayer(), true)
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