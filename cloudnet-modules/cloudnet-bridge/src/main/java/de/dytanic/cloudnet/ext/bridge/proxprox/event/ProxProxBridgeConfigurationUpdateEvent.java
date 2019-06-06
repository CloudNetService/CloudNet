package de.dytanic.cloudnet.ext.bridge.proxprox.event;

import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;

public final class ProxProxBridgeConfigurationUpdateEvent extends ProxProxBridgeEvent {

    private final BridgeConfiguration bridgeConfiguration;

    public ProxProxBridgeConfigurationUpdateEvent(BridgeConfiguration bridgeConfiguration) {
        this.bridgeConfiguration = bridgeConfiguration;
    }

    public BridgeConfiguration getBridgeConfiguration() {
        return this.bridgeConfiguration;
    }
}