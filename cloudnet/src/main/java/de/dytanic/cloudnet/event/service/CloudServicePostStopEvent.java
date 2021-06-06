package de.dytanic.cloudnet.event.service;

import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.service.ICloudService;

public final class CloudServicePostStopEvent extends DriverEvent {

  private final ICloudService cloudService;

  private final int exitValue;

  public CloudServicePostStopEvent(ICloudService cloudService, int exitValue) {
    this.cloudService = cloudService;
    this.exitValue = exitValue;
  }

  public ICloudService getCloudService() {
    return this.cloudService;
  }

  public int getExitValue() {
    return this.exitValue;
  }
}
