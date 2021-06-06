package de.dytanic.cloudnet.event.service;

import de.dytanic.cloudnet.driver.event.ICancelable;
import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.service.ICloudService;

public final class CloudServicePreDeleteEvent extends DriverEvent implements ICancelable {

  private final ICloudService cloudService;
  private boolean cancelled = false;

  public CloudServicePreDeleteEvent(ICloudService cloudService) {
    this.cloudService = cloudService;
  }

  public ICloudService getCloudService() {
    return this.cloudService;
  }

  @Override
  public boolean isCancelled() {
    return this.cancelled;
  }

  @Override
  public void setCancelled(boolean value) {
    this.cancelled = value;
  }
}
