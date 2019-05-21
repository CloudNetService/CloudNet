package de.dytanic.cloudnet.ext.bridge.nukkit.listener;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerLoginEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.nukkit.NukkitCloudNetHelper;
import de.dytanic.cloudnet.wrapper.Wrapper;

public final class NukkitPlayerListener implements Listener {

    @EventHandler
    public void handle(PlayerLoginEvent event)
    {
        BridgeHelper.sendChannelMessageServerLoginRequest(NukkitCloudNetHelper.createNetworkConnectionInfo(event.getPlayer()),
            NukkitCloudNetHelper.createNetworkPlayerServerInfo(event.getPlayer(), true));
    }

    @EventHandler
    public void handle(PlayerJoinEvent event)
    {
        BridgeHelper.sendChannelMessageServerLoginSuccess(NukkitCloudNetHelper.createNetworkConnectionInfo(event.getPlayer()),
            NukkitCloudNetHelper.createNetworkPlayerServerInfo(event.getPlayer(), false));

        BridgeHelper.updateServiceInfo();
    }

    @EventHandler
    public void handle(PlayerQuitEvent event)
    {
        BridgeHelper.sendChannelMessageServerDisconnect(NukkitCloudNetHelper.createNetworkConnectionInfo(event.getPlayer()),
            NukkitCloudNetHelper.createNetworkPlayerServerInfo(event.getPlayer(), false));

        Wrapper.getInstance().runTask(new Runnable() {
            @Override
            public void run()
            {
                BridgeHelper.updateServiceInfo();
            }
        });
    }
}