package de.dytanic.cloudnet.ext.bridge.gomint.event;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

public final class GoMintCloudServiceDisconnectNetworkEvent extends GoMintCloudNetEvent {

    private final ServiceInfoSnapshot serviceInfoSnapshot;

    public GoMintCloudServiceDisconnectNetworkEvent(ServiceInfoSnapshot serviceInfoSnapshot) {
        this.serviceInfoSnapshot = serviceInfoSnapshot;
    }

    public ServiceInfoSnapshot getServiceInfoSnapshot() {
        return this.serviceInfoSnapshot;
    }
}