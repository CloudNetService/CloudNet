package de.dytanic.cloudnet.ext.bridge.event;

import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.ext.bridge.player.ICloudPlayer;

public final class BridgeUpdateCloudPlayerEvent extends DriverEvent {

    private final ICloudPlayer cloudPlayer;

    public BridgeUpdateCloudPlayerEvent(ICloudPlayer cloudPlayer) {
        this.cloudPlayer = cloudPlayer;
    }

    public ICloudPlayer getCloudPlayer() {
        return this.cloudPlayer;
    }
}