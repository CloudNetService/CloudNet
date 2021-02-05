package de.dytanic.cloudnet.ext.bridge.waterdogpe.listener;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.proxy.BridgeProxyHelper;
import de.dytanic.cloudnet.ext.bridge.waterdogpe.WaterdogPECloudNetHelper;
import dev.waterdog.ProxyServer;
import dev.waterdog.event.EventManager;
import dev.waterdog.event.defaults.PlayerDisconnectEvent;
import dev.waterdog.event.defaults.PlayerLoginEvent;
import dev.waterdog.event.defaults.PlayerPreLoginEvent;
import dev.waterdog.event.defaults.PostTransferCompleteEvent;
import dev.waterdog.event.defaults.PreTransferEvent;
import dev.waterdog.player.ProxiedPlayer;

public final class WaterdogPEPlayerListener {

    public WaterdogPEPlayerListener() {
        EventManager eventManager = ProxyServer.getInstance().getEventManager();

        eventManager.subscribe(PlayerPreLoginEvent.class, event -> {
            String kickReason = BridgeHelper.sendChannelMessageProxyLoginRequest(WaterdogPECloudNetHelper.createNetworkConnectionInfo(event.getLoginData()));
            if (kickReason != null) {
                event.setCancelled(true);
                event.setCancelReason(kickReason);
            }
        });

        eventManager.subscribe(PlayerLoginEvent.class, event -> {
            BridgeHelper.sendChannelMessageProxyLoginSuccess(WaterdogPECloudNetHelper.createNetworkConnectionInfo(event.getPlayer().getLoginData()));
            BridgeHelper.updateServiceInfo();
        });

        eventManager.subscribe(PreTransferEvent.class, event -> {
            ProxiedPlayer proxiedPlayer = event.getPlayer();

            ServiceInfoSnapshot serviceInfoSnapshot = BridgeProxyHelper.getCachedServiceInfoSnapshot(event.getTargetServer().getServerName());

            if (serviceInfoSnapshot != null) {
                BridgeHelper.sendChannelMessageProxyServerConnectRequest(
                        WaterdogPECloudNetHelper.createNetworkConnectionInfo(proxiedPlayer.getLoginData()),
                        BridgeHelper.createNetworkServiceInfo(serviceInfoSnapshot)
                );
            }
        });

        eventManager.subscribe(PostTransferCompleteEvent.class, event -> {
            ServiceInfoSnapshot serviceInfoSnapshot = BridgeProxyHelper.getCachedServiceInfoSnapshot(event.getPlayer().getServer().getInfo().getServerName());

            if (serviceInfoSnapshot != null) {
                BridgeHelper.sendChannelMessageProxyServerSwitch(
                        WaterdogPECloudNetHelper.createNetworkConnectionInfo(event.getPlayer().getLoginData()),
                        BridgeHelper.createNetworkServiceInfo(serviceInfoSnapshot)
                );
            }
        });

        eventManager.subscribe(PlayerDisconnectEvent.class, event -> {
            BridgeHelper.sendChannelMessageProxyDisconnect(WaterdogPECloudNetHelper.createNetworkConnectionInfo(event.getPlayer().getLoginData()));
            BridgeProxyHelper.clearFallbackProfile(event.getPlayer().getUniqueId());

            ProxyServer.getInstance().getScheduler().scheduleDelayed(BridgeHelper::updateServiceInfo, 1);
        });
    }

}