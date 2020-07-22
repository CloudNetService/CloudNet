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

    private final BridgeConfiguration bridgeConfiguration;

    private final OnlyProxyProtection onlyProxyProtection;

    public NukkitPlayerListener() {
        this.bridgeConfiguration = BridgeConfigurationProvider.load();
        this.onlyProxyProtection = new OnlyProxyProtection(Server.getInstance().getPropertyBoolean("xbox-auth"));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void handle(PlayerLoginEvent event) {
        Player player = event.getPlayer();

        if (this.onlyProxyProtection.shouldDisallowPlayer(player.getAddress())) {
            event.setCancelled(true);
            event.setKickMessage(this.bridgeConfiguration.getMessages().get("server-join-cancel-because-only-proxy").replace('&', '§'));
            return;
        }

        String currentTaskName = Wrapper.getInstance().getServiceId().getTaskName();
        ServiceTask serviceTask = Wrapper.getInstance().getServiceTaskProvider().getServiceTask(currentTaskName);

        if (serviceTask == null) {
            return;
        }

        // if the service has a field "requiredPermission" and the field is not null or empty and
        // the player has the has not the permission of that field -> disconnect him
        String requiredPermission = serviceTask.getProperties().get("requiredPermission", String.class);
        requiredPermission = (requiredPermission.equals("null")) ? null : requiredPermission;
        if (requiredPermission != null &&
                !player.hasPermission(serviceTask.getProperties().get("requiredPermission", String.class))) {
            event.setCancelled(true);
            event.setKickMessage(ChatColor.translateAlternateColorCodes('&', this.bridgeConfiguration.getMessages().get("server-join-cancel-because-permission")));
            return;
        }

        if (serviceTask.isMaintenance() && !player.hasPermission("cloudnet.bridge.maintenance")) {
            event.setCancelled(true);
            event.setKickMessage(this.bridgeConfiguration.getMessages().get("server-join-cancel-because-maintenance").replace('&', '§'));
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

        event.getPlayer().getServer().getScheduler().scheduleDelayedTask(new Task() {
            @Override
            public void onRun(int currentTick) {
                BridgeHelper.updateServiceInfo();
            }
        }, 1);
    }

}