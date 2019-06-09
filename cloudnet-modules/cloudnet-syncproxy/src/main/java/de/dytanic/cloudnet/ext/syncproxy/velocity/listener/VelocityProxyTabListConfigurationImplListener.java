package de.dytanic.cloudnet.ext.syncproxy.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyTabListConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.velocity.VelocityCloudNetSyncProxyPlugin;
import de.dytanic.cloudnet.wrapper.Wrapper;

public final class VelocityProxyTabListConfigurationImplListener {

    @Subscribe
    public void handle(PostLoginEvent event) {
        SyncProxyTabListConfiguration syncProxyTabListConfiguration = VelocityCloudNetSyncProxyPlugin.getInstance().getTabListConfiguration();

        if (syncProxyTabListConfiguration != null && !VelocityCloudNetSyncProxyPlugin.getInstance().getProxyServer()
                .getPluginManager().getPlugin("cloudnet_bridge_velocity").isPresent())
            Wrapper.getInstance().publishServiceInfoUpdate();
    }

    @Subscribe
    public void handle(ServerConnectedEvent event) {
        SyncProxyTabListConfiguration syncProxyTabListConfiguration = VelocityCloudNetSyncProxyPlugin.getInstance().getTabListConfiguration();

        if (syncProxyTabListConfiguration != null)
            VelocityCloudNetSyncProxyPlugin.getInstance().setTabList(event.getPlayer());
    }

    @Subscribe
    public void handle(DisconnectEvent event) {
        SyncProxyTabListConfiguration syncProxyTabListConfiguration = VelocityCloudNetSyncProxyPlugin.getInstance().getTabListConfiguration();

        if (syncProxyTabListConfiguration != null && !VelocityCloudNetSyncProxyPlugin.getInstance().getProxyServer()
                .getPluginManager().getPlugin("cloudnet_bridge_velocity").isPresent())
            Wrapper.getInstance().publishServiceInfoUpdate();
    }
}