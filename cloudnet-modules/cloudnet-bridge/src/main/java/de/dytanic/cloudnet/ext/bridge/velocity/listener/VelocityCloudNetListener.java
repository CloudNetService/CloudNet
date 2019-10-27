package de.dytanic.cloudnet.ext.bridge.velocity.listener;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.event.events.instance.CloudNetTickEvent;
import de.dytanic.cloudnet.driver.event.events.network.NetworkChannelPacketReceiveEvent;
import de.dytanic.cloudnet.driver.event.events.network.NetworkClusterNodeInfoUpdateEvent;
import de.dytanic.cloudnet.driver.event.events.service.*;
import de.dytanic.cloudnet.ext.bridge.BridgeConstants;
import de.dytanic.cloudnet.ext.bridge.event.*;
import de.dytanic.cloudnet.ext.bridge.velocity.VelocityCloudNetHelper;
import de.dytanic.cloudnet.ext.bridge.velocity.event.*;
import de.dytanic.cloudnet.wrapper.event.service.ServiceInfoSnapshotConfigureEvent;
import net.kyori.text.TextComponent;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.UUID;

public final class VelocityCloudNetListener {

    @EventListener
    public void handle(ServiceInfoSnapshotConfigureEvent event) {
        VelocityCloudNetHelper.initProperties(event.getServiceInfoSnapshot());
        this.velocityCall(new VelocityServiceInfoSnapshotConfigureEvent(event.getServiceInfoSnapshot()));
    }

    @EventListener
    public void handle(CloudServiceStartEvent event) {
        if (VelocityCloudNetHelper.isServiceEnvironmentTypeProvidedForVelocity(event.getServiceInfo())) {
            if (event.getServiceInfo().getProperties().contains("Online-Mode") && event.getServiceInfo().getProperties().getBoolean("Online-Mode")) {
                return;
            }

            String name = event.getServiceInfo().getServiceId().getName();
            VelocityCloudNetHelper.getProxyServer().registerServer(new ServerInfo(name, new InetSocketAddress(
                    event.getServiceInfo().getAddress().getHost(),
                    event.getServiceInfo().getAddress().getPort()
            )));

            VelocityCloudNetHelper.addServerToVelocityPrioritySystemConfiguration(event.getServiceInfo(), name);
        }

        this.velocityCall(new VelocityCloudServiceStartEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceStopEvent event) {
        if (VelocityCloudNetHelper.isServiceEnvironmentTypeProvidedForVelocity(event.getServiceInfo())) {
            String name = event.getServiceInfo().getServiceId().getName();

            if (VelocityCloudNetHelper.getProxyServer().getServer(name).isPresent()) {
                VelocityCloudNetHelper.getProxyServer().unregisterServer(VelocityCloudNetHelper.getProxyServer().getServer(name).get().getServerInfo());
            }

            VelocityCloudNetHelper.removeServerToVelocityPrioritySystemConfiguration(event.getServiceInfo(), name);
            VelocityCloudNetHelper.SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION.put(event.getServiceInfo().getServiceId().getName(), event.getServiceInfo());
        }

        this.velocityCall(new VelocityCloudServiceStopEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceInfoUpdateEvent event) {
        if (VelocityCloudNetHelper.isServiceEnvironmentTypeProvidedForVelocity(event.getServiceInfo())) {
            VelocityCloudNetHelper.SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION.put(event.getServiceInfo().getServiceId().getName(), event.getServiceInfo());
        }

        this.velocityCall(new VelocityCloudServiceInfoUpdateEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceRegisterEvent event) {
        if (VelocityCloudNetHelper.isServiceEnvironmentTypeProvidedForVelocity(event.getServiceInfo())) {
            VelocityCloudNetHelper.SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION.put(event.getServiceInfo().getServiceId().getName(), event.getServiceInfo());
        }

        this.velocityCall(new VelocityCloudServiceRegisterEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceConnectNetworkEvent event) {
        if (VelocityCloudNetHelper.isServiceEnvironmentTypeProvidedForVelocity(event.getServiceInfo())) {
            VelocityCloudNetHelper.SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION.put(event.getServiceInfo().getServiceId().getName(), event.getServiceInfo());
        }

        this.velocityCall(new VelocityCloudServiceConnectNetworkEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceDisconnectNetworkEvent event) {
        if (VelocityCloudNetHelper.isServiceEnvironmentTypeProvidedForVelocity(event.getServiceInfo())) {
            VelocityCloudNetHelper.SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION.put(event.getServiceInfo().getServiceId().getName(), event.getServiceInfo());
        }

        this.velocityCall(new VelocityCloudServiceDisconnectNetworkEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceUnregisterEvent event) {
        if (VelocityCloudNetHelper.isServiceEnvironmentTypeProvidedForVelocity(event.getServiceInfo())) {
            VelocityCloudNetHelper.SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION.remove(event.getServiceInfo().getServiceId().getName());
        }

        this.velocityCall(new VelocityCloudServiceUnregisterEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(ChannelMessageReceiveEvent event) {
        this.velocityCall(new VelocityChannelMessageReceiveEvent(event.getChannel(), event.getMessage(), event.getData()));

        if (!event.getChannel().equalsIgnoreCase(BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME)) {
            return;
        }

        switch (event.getMessage().toLowerCase()) {
            case "send_on_proxy_player_to_server": {
                Player player = getPlayer(event.getData());

                if (player != null && event.getData().getString("serviceName") != null) {
                    Optional<RegisteredServer> serverInfo = VelocityCloudNetHelper.getProxyServer().getServer(event.getData().getString("serviceName"));

                    if (serverInfo != null && serverInfo.isPresent()) {
                        player.createConnectionRequest(serverInfo.get()).connect();
                    }
                }
            }
            break;
            case "kick_on_proxy_player_from_network": {
                Player player = getPlayer(event.getData());

                if (player != null && event.getData().getString("kickMessage") != null) {
                    player.disconnect(TextComponent.of((event.getData().getString("kickMessage") + "").replace("&", "§")));
                }
            }
            break;
            case "send_message_to_proxy_player": {
                Player player = getPlayer(event.getData());

                if (player != null && event.getData().getString("message") != null) {
                    player.sendMessage(TextComponent.of((event.getData().getString("message") + "").replace("&", "§")));
                }
            }
            break;
            case "broadcast_message": {
                String permission = event.getData().getString("permission");

                if (event.getData().getString("message") != null) {
                    TextComponent message = TextComponent.of(event.getData().getString("message").replace("&", "§"));
                    for (Player player : VelocityCloudNetHelper.getProxyServer().getAllPlayers()) {
                        if (permission == null || player.hasPermission(permission)) {
                            player.sendMessage(message);
                        }
                    }
                }
            }
            break;
        }
    }

    private Player getPlayer(JsonDocument data) {
        return Iterables.first(VelocityCloudNetHelper.getProxyServer().getAllPlayers(), player -> data.contains("uniqueId") && player.getUniqueId().equals(data.get("uniqueId", UUID.class)) ||
                data.contains("name") && player.getUsername().equalsIgnoreCase(data.getString("name")));
    }

    @EventListener
    public void handle(CloudNetTickEvent event) {
        this.velocityCall(new VelocityCloudNetTickEvent());
    }

    @EventListener
    public void handle(NetworkClusterNodeInfoUpdateEvent event) {
        this.velocityCall(new VelocityNetworkClusterNodeInfoUpdateEvent(event.getNetworkClusterNodeInfoSnapshot()));
    }

    @EventListener
    public void handle(NetworkChannelPacketReceiveEvent event) {
        this.velocityCall(new VelocityNetworkChannelPacketReceiveEvent(event.getChannel(), event.getPacket()));
    }

    @EventListener
    public void handle(BridgeConfigurationUpdateEvent event) {
        this.velocityCall(new VelocityBridgeConfigurationUpdateEvent(event.getBridgeConfiguration()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerLoginRequestEvent event) {
        this.velocityCall(new VelocityBridgeProxyPlayerLoginSuccessEvent(event.getNetworkConnectionInfo()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerLoginSuccessEvent event) {
        this.velocityCall(new VelocityBridgeProxyPlayerLoginSuccessEvent(event.getNetworkConnectionInfo()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerServerConnectRequestEvent event) {
        this.velocityCall(new VelocityBridgeProxyPlayerServerConnectRequestEvent(event.getNetworkConnectionInfo(), event.getNetworkServiceInfo()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerServerSwitchEvent event) {
        this.velocityCall(new VelocityBridgeProxyPlayerServerSwitchEvent(event.getNetworkConnectionInfo(), event.getNetworkServiceInfo()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerDisconnectEvent event) {
        this.velocityCall(new VelocityBridgeProxyPlayerDisconnectEvent(event.getNetworkConnectionInfo()));
    }

    @EventListener
    public void handle(BridgeServerPlayerLoginRequestEvent event) {
        this.velocityCall(new VelocityBridgeServerPlayerLoginRequestEvent(event.getNetworkConnectionInfo(), event.getNetworkPlayerServerInfo()));
    }

    @EventListener
    public void handle(BridgeServerPlayerLoginSuccessEvent event) {
        this.velocityCall(new VelocityBridgeServerPlayerLoginSuccessEvent(event.getNetworkConnectionInfo(), event.getNetworkPlayerServerInfo()));
    }

    @EventListener
    public void handle(BridgeServerPlayerDisconnectEvent event) {
        this.velocityCall(new VelocityBridgeServerPlayerDisconnectEvent(event.getNetworkConnectionInfo(), event.getNetworkPlayerServerInfo()));
    }

    private void velocityCall(Object event) {
        VelocityCloudNetHelper.getProxyServer().getEventManager().fire(event);
    }

}