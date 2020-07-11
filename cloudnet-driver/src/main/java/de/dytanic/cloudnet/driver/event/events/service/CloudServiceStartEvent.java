package de.dytanic.cloudnet.driver.event.events.service;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

/**
 * This event will be called when a service is started.
 * It will NEVER be called for local services on a node.
 */
public class CloudServiceStartEvent extends CloudServiceEvent {

    public CloudServiceStartEvent(ServiceInfoSnapshot serviceInfo) {
        super(serviceInfo);
    }
}