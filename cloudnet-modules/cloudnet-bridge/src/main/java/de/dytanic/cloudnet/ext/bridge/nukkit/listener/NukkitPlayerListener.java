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
import de.dytanic.cloudnet.ext.bridge.nukkit.NukkitCloudNetHelper;
import de.dytanic.cloudnet.ext.bridge.nukkit.event.NukkitServiceTaskAddEvent;
import de.dytanic.cloudnet.ext.bridge.server.OnlyProxyProtection;
import de.dytanic.cloudnet.wrapper.Wrapper;

import java.util.Optional;

public final class NukkitPlayerListener implements Listener {

    private final OnlyProxyProtection onlyProxyProtection;

    private ServiceTask serviceTask;

    public NukkitPlayerListener() {
        this.onlyProxyProtection = new OnlyProxyProtection(Server.getInstance().getPropertyBoolean("xbox-auth"));

        String currentTaskName = Wrapper.getInstance().getServiceId().getTaskName();
        this.serviceTask = Wrapper.getInstance().getServiceTaskProvider().getServiceTask(currentTaskName);
    }

    private Optional<String> getPlayerKickMessage(Player player) {
        if (this.serviceTask == null) {
            return Optional.empty();
        }

        BridgeConfiguration bridgeConfiguration = BridgeConfigurationProvider.load();

        if (this.serviceTask.isMaintenance() && !player.hasPermission("cloudnet.bridge.maintenance")) {
            return Optional.of(bridgeConfiguration.getMessages().get("server-join-cancel-because-maintenance").replace('&', '§'));
        } else {
            String requiredPermission = this.serviceTask.getProperties().getString("requiredPermission");
            if (requiredPermission != null && !player.hasPermission(requiredPermission)) {
                return Optional.of(bridgeConfiguration.getMessages().get("server-join-cancel-because-permission").replace('&', '§'));
            }
        }

        return Optional.empty();
    }

    @EventHandler
    public void handle(NukkitServiceTaskAddEvent event) {
        ServiceTask task = event.getTask();

        if (this.serviceTask != null && this.serviceTask.getName().equals(task.getName())) {
            this.serviceTask = task;

            Server.getInstance().getOnlinePlayers().forEach((uuid, player) -> this.getPlayerKickMessage(player).ifPresent(player::kick));
        }
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

        Optional<String> kickMessageOptional = this.getPlayerKickMessage(player);

        if (kickMessageOptional.isPresent()) {
            String kickMessage = kickMessageOptional.get();

            event.setCancelled(true);
            event.setKickMessage(kickMessage);
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