package de.dytanic.cloudnet.ext.bridge.gomint.event;

import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;

public final class GoMintBridgeConfigurationUpdateEvent extends GoMintBridgeEvent {

    private final BridgeConfiguration bridgeConfiguration;

    public GoMintBridgeConfigurationUpdateEvent(BridgeConfiguration bridgeConfiguration) {
        this.bridgeConfiguration = bridgeConfiguration;
    }

    public BridgeConfiguration getBridgeConfiguration() {
        return this.bridgeConfiguration;
    }
}