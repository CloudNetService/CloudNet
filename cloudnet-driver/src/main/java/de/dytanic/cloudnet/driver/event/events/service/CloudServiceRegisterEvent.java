package de.dytanic.cloudnet.driver.event.events.service;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

public final class CloudServiceRegisterEvent extends CloudServiceEvent {

    public CloudServiceRegisterEvent(ServiceInfoSnapshot serviceInfo) {
        super(serviceInfo);
    }
}