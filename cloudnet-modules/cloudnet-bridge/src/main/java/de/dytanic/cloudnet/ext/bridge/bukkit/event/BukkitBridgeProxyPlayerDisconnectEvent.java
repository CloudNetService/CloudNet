package de.dytanic.cloudnet.ext.bridge.bukkit.event;

import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.HandlerList;

@Getter
@RequiredArgsConstructor
public final class BukkitBridgeProxyPlayerDisconnectEvent extends BukkitBridgeEvent {

    private final NetworkConnectionInfo networkConnectionInfo;

    @Getter
    private static HandlerList handlerList = new HandlerList();

    @Override
    public HandlerList getHandlers()
    {
        return handlerList;
    }

}