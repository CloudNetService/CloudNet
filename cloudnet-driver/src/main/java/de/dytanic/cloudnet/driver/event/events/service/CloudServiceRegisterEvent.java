package de.dytanic.cloudnet.driver.event.events.service;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

/**
 * This event will be called when a service is registered. It will NEVER be called for local services on a node.
 */
public final class CloudServiceRegisterEvent extends CloudServiceEvent {

  public CloudServiceRegisterEvent(ServiceInfoSnapshot serviceInfo) {
    super(serviceInfo);
  }
}
