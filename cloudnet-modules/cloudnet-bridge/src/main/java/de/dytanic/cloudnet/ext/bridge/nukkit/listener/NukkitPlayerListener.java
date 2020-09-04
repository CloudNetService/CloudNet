package de.dytanic.cloudnet.ext.bridge.nukkit.listener;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerLoginEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.scheduler.Task;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;
import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.OnlyProxyProtection;
import de.dytanic.cloudnet.ext.bridge.nukkit.NukkitCloudNetHelper;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.bukkit.ChatColor;

public final class NukkitPlayerListener implements Listener {

    private final OnlyProxyProtection onlyProxyProtection;

    public NukkitPlayerListener() {
        this.onlyProxyProtection = new OnlyProxyProtection(Server.getInstance().getPropertyBoolean("xbox-auth"));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void handle(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        BridgeConfiguration bridgeConfiguration = BridgeConfigurationProvider.load();

        if (this.onlyProxyProtection.shouldDisallowPlayer(player.getAddress())) {
            event.setCancelled(true);
            event.setKickMessage(bridgeConfiguration.getMessages().get("server-join-cancel-because-only-proxy").replace('&', '§'));
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
                event.setKickMessage(bridgeConfiguration.getMessages().get("server-join-cancel-because-maintenance").replace('&', '§'));
                return;
            }
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

        event.getPlayer().getServer().getScheduler().scheduleDelayedTask(new Task() {
            @Override
            public void onRun(int currentTick) {
                BridgeHelper.updateServiceInfo();
            }
        }, 1);
    }

}