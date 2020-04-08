package de.dytanic.cloudnet.ext.syncproxy.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyCloudNetListener;
import de.dytanic.cloudnet.ext.syncproxy.velocity.listener.VelocitySyncProxyPlayerListener;
import de.dytanic.cloudnet.wrapper.Wrapper;

@Plugin(id = "cloudnet_syncproxy_velocity")
public final class VelocityCloudNetSyncProxyPlugin {

    private final ProxyServer proxyServer;

    @Inject
    public VelocityCloudNetSyncProxyPlugin(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    @Subscribe
    public void handleProxyInit(ProxyInitializeEvent event) {
        VelocitySyncProxyManagement syncProxyManagement = new VelocitySyncProxyManagement(this.proxyServer, this);

        CloudNetDriver.getInstance().getEventManager().registerListener(new SyncProxyCloudNetListener(syncProxyManagement));

        this.proxyServer.getEventManager().register(this, new VelocitySyncProxyPlayerListener(syncProxyManagement));
    }

    @Subscribe
    public void handleProxyShutdown(ProxyShutdownEvent event) {
        CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
        Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());
    }

}