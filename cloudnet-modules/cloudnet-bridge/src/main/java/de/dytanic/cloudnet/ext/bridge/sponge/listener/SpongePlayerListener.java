package de.dytanic.cloudnet.ext.bridge.sponge.listener;

import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.sponge.SpongeCloudNetBridgePlugin;
import de.dytanic.cloudnet.ext.bridge.sponge.SpongeCloudNetHelper;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.scheduler.SpongeExecutorService;

public final class SpongePlayerListener {

    private SpongeExecutorService executorService;

    public SpongePlayerListener(SpongeCloudNetBridgePlugin plugin) {
        this.executorService = Sponge.getGame().getScheduler().createSyncExecutor(plugin);
    }

    @Listener
    public void handle(ClientConnectionEvent.Join event) {
        BridgeHelper.sendChannelMessageServerLoginSuccess(SpongeCloudNetHelper.createNetworkConnectionInfo(event.getTargetEntity()),
                SpongeCloudNetHelper.createNetworkPlayerServerInfo(event.getTargetEntity(), false)
        );

        this.executorService.execute(BridgeHelper::updateServiceInfo);
    }

    @Listener
    public void handle(ClientConnectionEvent.Disconnect event) {
        BridgeHelper.sendChannelMessageServerDisconnect(SpongeCloudNetHelper.createNetworkConnectionInfo(event.getTargetEntity()),
                SpongeCloudNetHelper.createNetworkPlayerServerInfo(event.getTargetEntity(), false));

        this.executorService.execute(BridgeHelper::updateServiceInfo);
    }
}