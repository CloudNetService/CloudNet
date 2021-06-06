package de.dytanic.cloudnet.driver.event.events.service;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

/**
 * This event will be called when a service is connected to their node. It will NEVER be called for local services on a
 * node.
 */
public final class CloudServiceConnectNetworkEvent extends CloudServiceEvent {

  public CloudServiceConnectNetworkEvent(ServiceInfoSnapshot serviceInfo) {
    super(serviceInfo);
  }
}
