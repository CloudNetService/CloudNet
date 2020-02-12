package de.dytanic.cloudnet.driver.event.events.service;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

/**
 * This event will be called when a service is disconnected from their node.
 * It will NEVER be called for local services on a node.
 */
public final class CloudServiceDisconnectNetworkEvent extends CloudServiceEvent {

    public CloudServiceDisconnectNetworkEvent(ServiceInfoSnapshot serviceInfo) {
        super(serviceInfo);
    }
}