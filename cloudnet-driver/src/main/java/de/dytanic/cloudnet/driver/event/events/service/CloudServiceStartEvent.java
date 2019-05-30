package de.dytanic.cloudnet.driver.event.events.service;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

public class CloudServiceStartEvent extends CloudServiceEvent {

  public CloudServiceStartEvent(ServiceInfoSnapshot serviceInfo) {
    super(serviceInfo);
  }
}