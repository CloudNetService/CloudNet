package de.dytanic.cloudnet.event.service;

import de.dytanic.cloudnet.driver.event.ICancelable;
import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.driver.service.ServiceRemoteInclusion;
import de.dytanic.cloudnet.service.ICloudService;
import java.net.URLConnection;

public final class CloudServicePreLoadInclusionEvent extends DriverEvent implements ICancelable {

  private final ICloudService cloudService;

  private final ServiceRemoteInclusion serviceRemoteInclusion;

  private final URLConnection connection;

  private boolean cancelled;

  public CloudServicePreLoadInclusionEvent(ICloudService cloudService, ServiceRemoteInclusion serviceRemoteInclusion,
    URLConnection connection) {
    this.cloudService = cloudService;
    this.serviceRemoteInclusion = serviceRemoteInclusion;
    this.connection = connection;
  }

  public ICloudService getCloudService() {
    return this.cloudService;
  }

  public ServiceRemoteInclusion getServiceRemoteInclusion() {
    return this.serviceRemoteInclusion;
  }

  public URLConnection getConnection() {
    return this.connection;
  }

  public boolean isCancelled() {
    return this.cancelled;
  }

  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }
}
