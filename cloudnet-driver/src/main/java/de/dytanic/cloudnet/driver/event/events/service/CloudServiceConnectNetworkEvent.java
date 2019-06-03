package de.dytanic.cloudnet.driver.event.events.service;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

public final class CloudServiceConnectNetworkEvent extends CloudServiceEvent {

    public CloudServiceConnectNetworkEvent(ServiceInfoSnapshot serviceInfo) {
        super(serviceInfo);
    }
}