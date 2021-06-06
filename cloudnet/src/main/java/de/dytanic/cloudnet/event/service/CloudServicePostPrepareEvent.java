package de.dytanic.cloudnet.event.service;

import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.service.ICloudService;

public final class CloudServicePostPrepareEvent extends DriverEvent {

  private final ICloudService cloudService;

  public CloudServicePostPrepareEvent(ICloudService cloudService) {
    this.cloudService = cloudService;
  }

  public ICloudService getCloudService() {
    return this.cloudService;
  }
}
