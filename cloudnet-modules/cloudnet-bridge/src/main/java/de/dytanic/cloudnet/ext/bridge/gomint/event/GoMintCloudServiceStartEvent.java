package de.dytanic.cloudnet.ext.bridge.gomint.event;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

public final class GoMintCloudServiceStartEvent extends GoMintCloudNetEvent {

    private final ServiceInfoSnapshot serviceInfoSnapshot;

    public GoMintCloudServiceStartEvent(ServiceInfoSnapshot serviceInfoSnapshot) {
        this.serviceInfoSnapshot = serviceInfoSnapshot;
    }

    public ServiceInfoSnapshot getServiceInfoSnapshot() {
        return this.serviceInfoSnapshot;
    }
}