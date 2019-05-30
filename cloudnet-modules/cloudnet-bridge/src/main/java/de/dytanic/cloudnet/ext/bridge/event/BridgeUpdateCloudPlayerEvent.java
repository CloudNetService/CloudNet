package de.dytanic.cloudnet.ext.bridge.event;

import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.ext.bridge.player.ICloudPlayer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class BridgeUpdateCloudPlayerEvent extends DriverEvent {

  private final ICloudPlayer cloudPlayer;

}