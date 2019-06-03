package de.dytanic.cloudnet.ext.bridge.proxprox.event;

import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class ProxProxBridgeConfigurationUpdateEvent extends ProxProxBridgeEvent {

    private final BridgeConfiguration bridgeConfiguration;

}