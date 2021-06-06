package de.dytanic.cloudnet.ext.bridge.event;

import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.ext.bridge.player.ICloudOfflinePlayer;

public final class BridgeUpdateCloudOfflinePlayerEvent extends DriverEvent {

  private final ICloudOfflinePlayer cloudOfflinePlayer;

  public BridgeUpdateCloudOfflinePlayerEvent(ICloudOfflinePlayer cloudOfflinePlayer) {
    this.cloudOfflinePlayer = cloudOfflinePlayer;
  }

  public ICloudOfflinePlayer getCloudOfflinePlayer() {
    return this.cloudOfflinePlayer;
  }
}
