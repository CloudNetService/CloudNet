package de.dytanic.cloudnet.ext.bridge.nukkit.listener;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerLoginEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;
import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.nukkit.NukkitCloudNetHelper;
import de.dytanic.cloudnet.wrapper.Wrapper;

public final class NukkitPlayerListener implements Listener {

    private final BridgeConfiguration bridgeConfiguration;

    private final boolean onlyProxyProtection;

    public NukkitPlayerListener() {
        this.bridgeConfiguration = BridgeConfigurationProvider.load();
        this.onlyProxyProtection = !Server.getInstance().getPropertyBoolean("xbox-auth")
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
            event.setCancelled(true);
            event.setKickMessage(this.bridgeConfiguration.getMessages().get("server-join-cancel-because-maintenance").replace('&', '§'));
            return;
        }

        if (this.onlyProxyProtection && !BridgeHelper.playerIsOnProxy(player.getUniqueId(), player.getAddress())) {
            event.setCancelled(true);
            event.setKickMessage(this.bridgeConfiguration.getMessages().get("server-join-cancel-because-only-proxy").replace('&', '§'));
            return;
        }

        BridgeHelper.sendChannelMessageServerLoginRequest(NukkitCloudNetHelper.createNetworkConnectionInfo(player),
                NukkitCloudNetHelper.createNetworkPlayerServerInfo(player, true)
        );
    }

    @EventHandler
    public void handle(PlayerJoinEvent event) {
        BridgeHelper.sendChannelMessageServerLoginSuccess(NukkitCloudNetHelper.createNetworkConnectionInfo(event.getPlayer()),
                NukkitCloudNetHelper.createNetworkPlayerServerInfo(event.getPlayer(), false));

        BridgeHelper.updateServiceInfo();
    }

    @EventHandler
    public void handle(PlayerQuitEvent event) {
        BridgeHelper.sendChannelMessageServerDisconnect(NukkitCloudNetHelper.createNetworkConnectionInfo(event.getPlayer()),
                NukkitCloudNetHelper.createNetworkPlayerServerInfo(event.getPlayer(), false));

        Wrapper.getInstance().runTask(BridgeHelper::updateServiceInfo);
    }

}