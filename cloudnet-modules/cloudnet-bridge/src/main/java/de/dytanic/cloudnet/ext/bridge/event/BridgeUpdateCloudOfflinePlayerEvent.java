package de.dytanic.cloudnet.ext.bridge.event;

import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.ext.bridge.player.ICloudOfflinePlayer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class BridgeUpdateCloudOfflinePlayerEvent extends DriverEvent {

  private final ICloudOfflinePlayer cloudOfflinePlayer;

}