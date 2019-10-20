package de.dytanic.cloudnet.ext.bridge.bukkit;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.bukkit.listener.BukkitCloudNetListener;
import de.dytanic.cloudnet.ext.bridge.bukkit.listener.BukkitPlayerListener;
import de.dytanic.cloudnet.ext.bridge.listener.BridgeCustomChannelMessageListener;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.InetSocketAddress;

public final class BukkitCloudNetBridgePlugin extends JavaPlugin {

    @Override
    public synchronized void onEnable() {
        BukkitCloudNetHelper.setPlugin(this);
        this.initListeners();

        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(this, "cloudnet:main");
        BridgeHelper.updateServiceInfo();

        Bukkit.getScheduler().runTaskTimer(this, this::runFireServerListPingEvent, 0, 10);
    }

    @Override
    public synchronized void onDisable() {
        HandlerList.unregisterAll(this);
        CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
        Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());
    }

    private void initListeners() {
        //BukkitAPI
        Bukkit.getServer().getPluginManager().registerEvents(new BukkitPlayerListener(), this);

        //CloudNet
        CloudNetDriver.getInstance().getEventManager().registerListener(new BukkitCloudNetListener());
        CloudNetDriver.getInstance().getEventManager().registerListener(new BridgeCustomChannelMessageListener());
    }

    private void runFireServerListPingEvent() {
        ServerListPingEvent serverListPingEvent = new ServerListPingEvent(
                new InetSocketAddress("127.0.0.1", 53345).getAddress(),
                BukkitCloudNetHelper.getApiMotd(),
                Bukkit.getOnlinePlayers().size(),
                BukkitCloudNetHelper.getMaxPlayers()
        );

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            boolean hasToUpdate = false, value = false;

            try {
                Bukkit.getPluginManager().callEvent(serverListPingEvent);

                if (!serverListPingEvent.getMotd().equalsIgnoreCase(BukkitCloudNetHelper.getApiMotd())) {
                    hasToUpdate = true;

                    BukkitCloudNetHelper.setApiMotd(serverListPingEvent.getMotd());
                    if (serverListPingEvent.getMotd().toLowerCase().contains("running") ||
                            serverListPingEvent.getMotd().toLowerCase().contains("ingame") ||
                            serverListPingEvent.getMotd().toLowerCase().contains("playing")) {
                        value = true;
                    }
                }

                if (serverListPingEvent.getMaxPlayers() != BukkitCloudNetHelper.getMaxPlayers()) {
                    hasToUpdate = true;
                    BukkitCloudNetHelper.setMaxPlayers(serverListPingEvent.getMaxPlayers());
                }

                if (value) {
                    BukkitCloudNetHelper.changeToIngame();
                    return;
                }

                if (hasToUpdate) {
                    BridgeHelper.updateServiceInfo();
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });
    }
}
