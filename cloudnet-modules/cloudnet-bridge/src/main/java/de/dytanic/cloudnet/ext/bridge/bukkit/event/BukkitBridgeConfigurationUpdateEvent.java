package de.dytanic.cloudnet.ext.bridge.bukkit.event;

import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;
import org.bukkit.event.HandlerList;

public final class BukkitBridgeConfigurationUpdateEvent extends BukkitBridgeEvent {

    private static HandlerList handlerList = new HandlerList();
    private final BridgeConfiguration bridgeConfiguration;

    public BukkitBridgeConfigurationUpdateEvent(BridgeConfiguration bridgeConfiguration) {
        this.bridgeConfiguration = bridgeConfiguration;
    }

    public static HandlerList getHandlerList() {
        return BukkitBridgeConfigurationUpdateEvent.handlerList;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public BridgeConfiguration getBridgeConfiguration() {
        return this.bridgeConfiguration;
    }
}