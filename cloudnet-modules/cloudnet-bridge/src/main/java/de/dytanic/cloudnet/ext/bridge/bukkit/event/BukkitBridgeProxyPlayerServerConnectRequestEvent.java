package de.dytanic.cloudnet.ext.bridge.bukkit.event;

import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import de.dytanic.cloudnet.ext.bridge.player.NetworkServiceInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.HandlerList;

@Getter
@RequiredArgsConstructor
public class BukkitBridgeProxyPlayerServerConnectRequestEvent extends BukkitBridgeEvent {

    @Getter
    private static HandlerList handlerList = new HandlerList();
    private final NetworkConnectionInfo networkConnectionInfo;
    private final NetworkServiceInfo networkServiceInfo;

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

}