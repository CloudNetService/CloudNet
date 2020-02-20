package de.dytanic.cloudnet.ext.bridge.bukkit.event;

import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkServiceInfo;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class BukkitBridgeProxyPlayerServerConnectRequestEvent extends BukkitBridgeEvent {

    private static HandlerList handlerList = new HandlerList();
    private final NetworkConnectionInfo networkConnectionInfo;
    private final NetworkServiceInfo networkServiceInfo;

    public BukkitBridgeProxyPlayerServerConnectRequestEvent(NetworkConnectionInfo networkConnectionInfo, NetworkServiceInfo networkServiceInfo) {
        this.networkConnectionInfo = networkConnectionInfo;
        this.networkServiceInfo = networkServiceInfo;
    }

    public static HandlerList getHandlerList() {
        return BukkitBridgeProxyPlayerServerConnectRequestEvent.handlerList;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public NetworkConnectionInfo getNetworkConnectionInfo() {
        return this.networkConnectionInfo;
    }

    public NetworkServiceInfo getNetworkServiceInfo() {
        return this.networkServiceInfo;
    }
}