package de.dytanic.cloudnet.ext.bridge.event;

import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;

public final class BridgeConfigurationUpdateEvent extends DriverEvent {

  private final BridgeConfiguration bridgeConfiguration;

  public BridgeConfigurationUpdateEvent(BridgeConfiguration bridgeConfiguration) {
    this.bridgeConfiguration = bridgeConfiguration;
  }

  public BridgeConfiguration getBridgeConfiguration() {
    return this.bridgeConfiguration;
  }
}
