package de.dytanic.cloudnet.ext.bridge.bukkit.event;

import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.HandlerList;

@Getter
@RequiredArgsConstructor
public final class BukkitBridgeConfigurationUpdateEvent extends BukkitBridgeEvent {

    @Getter
    private static HandlerList handlerList = new HandlerList();
    private final BridgeConfiguration bridgeConfiguration;

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}