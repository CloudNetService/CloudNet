package de.dytanic.cloudnet.ext.bridge.velocity.event;

import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;

public final class VelocityBridgeConfigurationUpdateEvent extends VelocityBridgeEvent {

    private final BridgeConfiguration bridgeConfiguration;

    public VelocityBridgeConfigurationUpdateEvent(BridgeConfiguration bridgeConfiguration) {
        this.bridgeConfiguration = bridgeConfiguration;
    }

    public BridgeConfiguration getBridgeConfiguration() {
        return this.bridgeConfiguration;
    }
}