package de.dytanic.cloudnet.ext.bridge.nukkit.event;

import cn.nukkit.event.HandlerList;
import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;

/**
 * {@inheritDoc}
 */
public final class NukkitBridgeConfigurationUpdateEvent extends NukkitBridgeEvent {

    private static final HandlerList handlers = new HandlerList();

    private final BridgeConfiguration bridgeConfiguration;

    public NukkitBridgeConfigurationUpdateEvent(BridgeConfiguration bridgeConfiguration) {
        this.bridgeConfiguration = bridgeConfiguration;
    }

    public static HandlerList getHandlers() {
        return NukkitBridgeConfigurationUpdateEvent.handlers;
    }

    public BridgeConfiguration getBridgeConfiguration() {
        return this.bridgeConfiguration;
    }
}