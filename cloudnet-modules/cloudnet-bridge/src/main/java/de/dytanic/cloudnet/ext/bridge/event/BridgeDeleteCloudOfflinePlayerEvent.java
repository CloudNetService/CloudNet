package de.dytanic.cloudnet.ext.bridge.event;

import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.ext.bridge.player.ICloudOfflinePlayer;

public final class BridgeDeleteCloudOfflinePlayerEvent extends DriverEvent {

  private final ICloudOfflinePlayer cloudPlayer;

  public BridgeDeleteCloudOfflinePlayerEvent(ICloudOfflinePlayer cloudPlayer) {
    this.cloudPlayer = cloudPlayer;
  }

  public ICloudOfflinePlayer getCloudOfflinePlayer() {
    return this.cloudPlayer;
  }
}
