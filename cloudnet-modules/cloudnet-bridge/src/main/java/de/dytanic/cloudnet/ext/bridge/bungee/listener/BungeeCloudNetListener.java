package de.dytanic.cloudnet.ext.bridge.bungee.listener;

import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.event.events.network.NetworkChannelPacketReceiveEvent;
import de.dytanic.cloudnet.driver.event.events.network.NetworkClusterNodeInfoUpdateEvent;
import de.dytanic.cloudnet.driver.event.events.service.*;
import de.dytanic.cloudnet.ext.bridge.BridgeConstants;
import de.dytanic.cloudnet.ext.bridge.bungee.BungeeCloudNetHelper;
import de.dytanic.cloudnet.ext.bridge.bungee.event.*;
import de.dytanic.cloudnet.ext.bridge.event.*;
import de.dytanic.cloudnet.wrapper.event.service.ServiceInfoSnapshotConfigureEvent;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Event;
import net.md_5.bungee.chat.ComponentSerializer;

import java.net.InetSocketAddress;
import java.util.Base64;
import java.util.UUID;

public final class BungeeCloudNetListener {

    @EventListener
    public void handle(ServiceInfoSnapshotConfigureEvent event) {
        BungeeCloudNetHelper.initProperties(event.getServiceInfoSnapshot());
        this.bungeeCall(new BungeeServiceInfoSnapshotConfigureEvent(event.getServiceInfoSnapshot()));
    }

    @EventListener
    public void handle(CloudServiceStartEvent event) {
        if (BungeeCloudNetHelper.isServiceEnvironmentTypeProvidedForBungeeCord(event.getServiceInfo())) {
            if (event.getServiceInfo().getProperties().contains("Online-Mode") && event.getServiceInfo().getProperties().getBoolean("Online-Mode")) {
                return;
            }

            String name = event.getServiceInfo().getServiceId().getName();

            ProxyServer.getInstance().getServers().put(name, BungeeCloudNetHelper.createServerInfo(name, new InetSocketAddress(
                    event.getServiceInfo().getAddress().getHost(),
                    event.getServiceInfo().getAddress().getPort()
            )));
        }

        this.bungeeCall(new BungeeCloudServiceStartEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceStopEvent event) {
        if (BungeeCloudNetHelper.isServiceEnvironmentTypeProvidedForBungeeCord(event.getServiceInfo())) {
            String name = event.getServiceInfo().getServiceId().getName();
            ProxyServer.getInstance().getServers().remove(name);
        }

        this.bungeeCall(new BungeeCloudServiceStopEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceInfoUpdateEvent event) {
        if (BungeeCloudNetHelper.isServiceEnvironmentTypeProvidedForBungeeCord(event.getServiceInfo())) {
            BungeeCloudNetHelper.SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION.put(event.getServiceInfo().getServiceId().getName(), event.getServiceInfo());
        }

        this.bungeeCall(new BungeeCloudServiceInfoUpdateEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceRegisterEvent event) {
        if (BungeeCloudNetHelper.isServiceEnvironmentTypeProvidedForBungeeCord(event.getServiceInfo())) {
            BungeeCloudNetHelper.SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION.put(event.getServiceInfo().getServiceId().getName(), event.getServiceInfo());
        }

        this.bungeeCall(new BungeeCloudServiceRegisterEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceConnectNetworkEvent event) {
        if (BungeeCloudNetHelper.isServiceEnvironmentTypeProvidedForBungeeCord(event.getServiceInfo())) {
            BungeeCloudNetHelper.SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION.put(event.getServiceInfo().getServiceId().getName(), event.getServiceInfo());
        }

        this.bungeeCall(new BungeeCloudServiceConnectNetworkEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceDisconnectNetworkEvent event) {
        if (BungeeCloudNetHelper.isServiceEnvironmentTypeProvidedForBungeeCord(event.getServiceInfo())) {
            BungeeCloudNetHelper.SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION.put(event.getServiceInfo().getServiceId().getName(), event.getServiceInfo());
        }

        this.bungeeCall(new BungeeCloudServiceDisconnectNetworkEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(CloudServiceUnregisterEvent event) {
        if (BungeeCloudNetHelper.isServiceEnvironmentTypeProvidedForBungeeCord(event.getServiceInfo())) {
            BungeeCloudNetHelper.SERVER_TO_SERVICE_INFO_SNAPSHOT_ASSOCIATION.remove(event.getServiceInfo().getServiceId().getName());
        }

        this.bungeeCall(new BungeeCloudServiceUnregisterEvent(event.getServiceInfo()));
    }

    @EventListener
    public void handle(ChannelMessageReceiveEvent event) {
        this.bungeeCall(new BungeeChannelMessageReceiveEvent(event.getChannel(), event.getMessage(), event.getData()));

        if (!event.getChannel().equalsIgnoreCase(BridgeConstants.BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME)) {
            return;
        }

        switch (event.getMessage().toLowerCase()) {
            case "send_on_proxy_player_to_server": {
                ProxiedPlayer proxiedPlayer = getPlayer(event.getData());

                if (proxiedPlayer != null && event.getData().getString("serviceName") != null) {
                    ServerInfo serverInfo = ProxyServer.getInstance().getServerInfo(event.getData().getString("serviceName"));

                    if (serverInfo != null) {
                        proxiedPlayer.connect(serverInfo);
                    }
                }
            }
            break;
            case "kick_on_proxy_player_from_network": {
                ProxiedPlayer proxiedPlayer = getPlayer(event.getData());

                if (proxiedPlayer != null && event.getData().getString("kickMessage") != null) {
                    proxiedPlayer.disconnect(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', event.getData().getString("kickMessage"))));
                }
            }
            break;
            case "send_message_to_proxy_player": {
                ProxiedPlayer proxiedPlayer = getPlayer(event.getData());

                if (proxiedPlayer != null) {
                    BaseComponent[] messages = event.getData().contains("message") ? TextComponent.fromLegacyText(event.getData().getString("message")) :
                            ComponentSerializer.parse(event.getData().getString("messages"));
                    proxiedPlayer.sendMessage(messages);
                }
            }
            break;
            case "send_plugin_message_to_proxy_player": {
                ProxiedPlayer proxiedPlayer = getPlayer(event.getData());

                if (proxiedPlayer != null) {
                    byte[] data = Base64.getDecoder().decode(event.getData().getString("data"));
                    proxiedPlayer.sendData(event.getData().getString("tag"), data);
                }
            }
            break;

            case "broadcast_message": {
                String permission = event.getData().getString("permission");

                BaseComponent[] messages = event.getData().contains("message") ? TextComponent.fromLegacyText(event.getData().getString("message")) :
                        ComponentSerializer.parse(event.getData().getString("messages"));

                for (ProxiedPlayer proxiedPlayer : ProxyServer.getInstance().getPlayers()) {
                    if (permission == null || proxiedPlayer.hasPermission(permission)) {
                        proxiedPlayer.sendMessage(messages);
                    }
                }
            }
            break;
        }
    }

    public ProxiedPlayer getPlayer(JsonDocument data) {
        return Iterables.first(ProxyServer.getInstance().getPlayers(), proxiedPlayer ->
                data.contains("uniqueId") && proxiedPlayer.getUniqueId().equals(data.get("uniqueId", UUID.class)));
    }

    @EventListener
    public void handle(NetworkClusterNodeInfoUpdateEvent event) {
        this.bungeeCall(new BungeeNetworkClusterNodeInfoUpdateEvent(event.getNetworkClusterNodeInfoSnapshot()));
    }

    @EventListener
    public void handle(NetworkChannelPacketReceiveEvent event) {
        this.bungeeCall(new BungeeNetworkChannelPacketReceiveEvent(event.getChannel(), event.getPacket()));
    }

    @EventListener
    public void handle(BridgeConfigurationUpdateEvent event) {
        this.bungeeCall(new BungeeBridgeConfigurationUpdateEvent(event.getBridgeConfiguration()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerLoginRequestEvent event) {
        this.bungeeCall(new BungeeBridgeProxyPlayerLoginSuccessEvent(event.getNetworkConnectionInfo()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerLoginSuccessEvent event) {
        this.bungeeCall(new BungeeBridgeProxyPlayerLoginSuccessEvent(event.getNetworkConnectionInfo()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerServerConnectRequestEvent event) {
        this.bungeeCall(new BungeeBridgeProxyPlayerServerConnectRequestEvent(event.getNetworkConnectionInfo(), event.getNetworkServiceInfo()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerServerSwitchEvent event) {
        this.bungeeCall(new BungeeBridgeProxyPlayerServerSwitchEvent(event.getNetworkConnectionInfo(), event.getNetworkServiceInfo()));
    }

    @EventListener
    public void handle(BridgeProxyPlayerDisconnectEvent event) {
        this.bungeeCall(new BungeeBridgeProxyPlayerDisconnectEvent(event.getNetworkConnectionInfo()));
    }

    @EventListener
    public void handle(BridgeServerPlayerLoginRequestEvent event) {
        this.bungeeCall(new BungeeBridgeServerPlayerLoginRequestEvent(event.getNetworkConnectionInfo(), event.getNetworkPlayerServerInfo()));
    }

    @EventListener
    public void handle(BridgeServerPlayerLoginSuccessEvent event) {
        this.bungeeCall(new BungeeBridgeServerPlayerLoginSuccessEvent(event.getNetworkConnectionInfo(), event.getNetworkPlayerServerInfo()));
    }

    @EventListener
    public void handle(BridgeServerPlayerDisconnectEvent event) {
        this.bungeeCall(new BungeeBridgeServerPlayerDisconnectEvent(event.getNetworkConnectionInfo(), event.getNetworkPlayerServerInfo()));
    }

    private void bungeeCall(Event event) {
        ProxyServer.getInstance().getPluginManager().callEvent(event);
    }

}
