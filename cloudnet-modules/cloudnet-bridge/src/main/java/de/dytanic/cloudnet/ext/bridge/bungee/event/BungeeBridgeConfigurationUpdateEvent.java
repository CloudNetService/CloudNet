package de.dytanic.cloudnet.ext.bridge.bungee.event;

import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;

public final class BungeeBridgeConfigurationUpdateEvent extends BungeeBridgeEvent {

  private final BridgeConfiguration bridgeConfiguration;

  public BungeeBridgeConfigurationUpdateEvent(BridgeConfiguration bridgeConfiguration) {
    this.bridgeConfiguration = bridgeConfiguration;
  }

  public BridgeConfiguration getBridgeConfiguration() {
    return this.bridgeConfiguration;
  }
}
