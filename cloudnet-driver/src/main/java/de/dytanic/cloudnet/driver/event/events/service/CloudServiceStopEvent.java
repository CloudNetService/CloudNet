package de.dytanic.cloudnet.driver.event.events.service;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

public final class CloudServiceStopEvent extends CloudServiceEvent {

    public CloudServiceStopEvent(ServiceInfoSnapshot serviceInfo) {
        super(serviceInfo);
    }
}