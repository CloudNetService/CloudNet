package de.dytanic.cloudnet.ext.bridge.gomint.event;

import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class GoMintBridgeConfigurationUpdateEvent extends GoMintBridgeEvent {

    private final BridgeConfiguration bridgeConfiguration;

}