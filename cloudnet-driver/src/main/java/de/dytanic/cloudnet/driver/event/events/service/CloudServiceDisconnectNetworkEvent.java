package de.dytanic.cloudnet.driver.event.events.service;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

public final class CloudServiceDisconnectNetworkEvent extends
  CloudServiceEvent {

  public CloudServiceDisconnectNetworkEvent(ServiceInfoSnapshot serviceInfo) {
    super(serviceInfo);
  }
}