package de.dytanic.cloudnet.ext.bridge.velocity.event;

import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class VelocityBridgeConfigurationUpdateEvent extends
    VelocityBridgeEvent {

  private final BridgeConfiguration bridgeConfiguration;

}