package de.dytanic.cloudnet.ext.bridge.sponge.event;

import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class SpongeBridgeConfigurationUpdateEvent extends SpongeBridgeEvent {

    private final BridgeConfiguration bridgeConfiguration;

}