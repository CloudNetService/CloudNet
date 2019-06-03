package de.dytanic.cloudnet.driver.event.events.service;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

public final class CloudServiceUnregisterEvent extends CloudServiceEvent {

    public CloudServiceUnregisterEvent(ServiceInfoSnapshot serviceInfo) {
        super(serviceInfo);
    }
}
