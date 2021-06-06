package de.dytanic.cloudnet.ext.bridge.sponge.event;

import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;

public final class SpongeBridgeConfigurationUpdateEvent extends SpongeBridgeEvent {

  private final BridgeConfiguration bridgeConfiguration;

  public SpongeBridgeConfigurationUpdateEvent(BridgeConfiguration bridgeConfiguration) {
    this.bridgeConfiguration = bridgeConfiguration;
  }

  public BridgeConfiguration getBridgeConfiguration() {
    return this.bridgeConfiguration;
  }
}
