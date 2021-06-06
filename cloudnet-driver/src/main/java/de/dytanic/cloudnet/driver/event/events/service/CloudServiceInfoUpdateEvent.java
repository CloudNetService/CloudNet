package de.dytanic.cloudnet.driver.event.events.service;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

/**
 * This event will be called when any service is updated.
 */
public final class CloudServiceInfoUpdateEvent extends CloudServiceEvent {

  public CloudServiceInfoUpdateEvent(ServiceInfoSnapshot serviceInfo) {
    super(serviceInfo);
  }
}
