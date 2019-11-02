package de.dytanic.cloudnet.ext.bridge.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeHelper;
import de.dytanic.cloudnet.ext.bridge.player.NetworkServiceInfo;
import de.dytanic.cloudnet.ext.bridge.velocity.VelocityCloudNetHelper;
import de.dytanic.cloudnet.wrapper.Wrapper;
import net.kyori.text.TextComponent;

public final class VelocityPlayerListener {

    @Subscribe
    public void handle(LoginEvent event) {
        BridgeHelper.sendChannelMessageProxyLoginRequest(VelocityCloudNetHelper.createNetworkConnectionInfo(event.getPlayer()));
    }

    @Subscribe
    public void handle(PostLoginEvent event) {
        BridgeHelper.sendChannelMessageProxyLoginSuccess(VelocityCloudNetHelper.createNetworkConnectionInfo(event.getPlayer()));

        Wrapper.getInstance().runTask(VelocityCloudNetHelper::updateServiceInfo);
    }

    @Subscribe
    public void handle(ServerPreConnectEvent event) {
        if (!event.getPlayer().getCurrentServer().isPresent()) {
            String server = VelocityCloudNetHelper.filterServiceForPlayer(event.getPlayer(), null);

            if (server != null && VelocityCloudNetHelper.getProxyServer().getServer(server).isPresent()) {
                event.setResult(ServerPreConnectEvent.ServerResult.allowed(VelocityCloudNetHelper.getProxyServer().getServer(server).get()));
            }
        }

        ServiceInfoSnapshot serviceInfoSnapshot = VelocityCloudNetHelper.SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION.get(event.getResult().getServer().get().getServerInfo().getName());

        if (serviceInfoSnapshot != null) {
            BridgeHelper.sendChannelMessageProxyServerConnectRequest(VelocityCloudNetHelper.createNetworkConnectionInfo(event.getPlayer()),
                    new NetworkServiceInfo(serviceInfoSnapshot.getServiceId().getEnvironment(), serviceInfoSnapshot.getServiceId().getUniqueId(),
                            serviceInfoSnapshot.getServiceId().getName()));

            try {
                Thread.sleep(10);
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }
    }

    @Subscribe
    public void handle(ServerConnectedEvent event) {
        ServiceInfoSnapshot serviceInfoSnapshot = VelocityCloudNetHelper.SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION.get(event.getServer().getServerInfo().getName());

        if (serviceInfoSnapshot != null) {
            BridgeHelper.sendChannelMessageProxyServerSwitch(VelocityCloudNetHelper.createNetworkConnectionInfo(event.getPlayer()),
                    new NetworkServiceInfo(serviceInfoSnapshot.getServiceId().getEnvironment(), serviceInfoSnapshot.getServiceId().getUniqueId(),
                            serviceInfoSnapshot.getServiceId().getName()));
        }
    }

    @Subscribe
    public void handle(KickedFromServerEvent event) {
        String server = VelocityCloudNetHelper.filterServiceForPlayer(event.getPlayer(), event.getServer().getServerInfo().getName());

        if (VelocityCloudNetHelper.isFallbackServer(event.getServer().getServerInfo())) {
            event.getPlayer().disconnect(event.getOriginalReason().orElseGet(() -> TextComponent.of("§cNo reason given")));
            return;
        }

        event.getOriginalReason().ifPresent(component -> event.getPlayer().sendMessage(component));

        if (server != null && VelocityCloudNetHelper.getProxyServer().getServer(server).isPresent()) {
            event.setResult(KickedFromServerEvent.RedirectPlayer.create(VelocityCloudNetHelper.getProxyServer().getServer(server).get()));
        }
    }

    @Subscribe
    public void handle(DisconnectEvent event) {
        BridgeHelper.sendChannelMessageProxyDisconnect(VelocityCloudNetHelper.createNetworkConnectionInfo(event.getPlayer()));

        Wrapper.getInstance().runTask(VelocityCloudNetHelper::updateServiceInfo);
    }
}