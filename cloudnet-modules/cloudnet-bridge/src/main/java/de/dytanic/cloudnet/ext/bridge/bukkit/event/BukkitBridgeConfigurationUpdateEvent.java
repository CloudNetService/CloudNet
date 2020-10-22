package de.dytanic.cloudnet.ext.bridge.bukkit.event;

import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * {@inheritDoc}
 */
public final class BukkitBridgeConfigurationUpdateEvent extends BukkitBridgeEvent {

    private static final HandlerList handlerList = new HandlerList();
    private final BridgeConfiguration bridgeConfiguration;

    public BukkitBridgeConfigurationUpdateEvent(BridgeConfiguration bridgeConfiguration) {
        this.bridgeConfiguration = bridgeConfiguration;
    }

    public static HandlerList getHandlerList() {
        return BukkitBridgeConfigurationUpdateEvent.handlerList;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public BridgeConfiguration getBridgeConfiguration() {
        return this.bridgeConfiguration;
    }
}