package de.dytanic.cloudnet.ext.bridge.bungee.event;

import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class BungeeBridgeConfigurationUpdateEvent extends BungeeBridgeEvent {

    private final BridgeConfiguration bridgeConfiguration;

}