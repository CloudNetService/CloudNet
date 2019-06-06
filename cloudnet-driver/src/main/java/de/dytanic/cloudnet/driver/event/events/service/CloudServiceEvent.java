package de.dytanic.cloudnet.driver.event.events.service;

import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

public abstract class CloudServiceEvent extends DriverEvent {

    private final ServiceInfoSnapshot serviceInfo;

    public CloudServiceEvent(ServiceInfoSnapshot serviceInfo) {
        this.serviceInfo = serviceInfo;
    }

    public ServiceInfoSnapshot getServiceInfo() {
        return this.serviceInfo;
    }
}