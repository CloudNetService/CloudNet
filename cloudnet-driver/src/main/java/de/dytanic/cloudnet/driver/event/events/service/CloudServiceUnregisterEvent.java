package de.dytanic.cloudnet.driver.event.events.service;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

/**
 * This event will be called when a service is unregistered on any node or a node with registered services disconnects.
 * It will NEVER be called for local services on a node.
 */
public final class CloudServiceUnregisterEvent extends CloudServiceEvent {

  public CloudServiceUnregisterEvent(ServiceInfoSnapshot serviceInfo) {
    super(serviceInfo);
  }
}
