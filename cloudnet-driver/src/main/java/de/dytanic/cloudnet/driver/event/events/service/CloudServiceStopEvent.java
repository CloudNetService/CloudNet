package de.dytanic.cloudnet.driver.event.events.service;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

/**
 * This event will be called when a service is stopped.
 * It will NEVER be called for local services on a node.
 */
public final class CloudServiceStopEvent extends CloudServiceEvent {

    public CloudServiceStopEvent(ServiceInfoSnapshot serviceInfo) {
        super(serviceInfo);
    }
}