package de.dytanic.cloudnet.ext.bridge.gomint.event;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

/**
 * This event will only fire synchronously, because CloudNet has to initialize the GoMint service properties synchronously anyway.
 */
public final class GoMintServiceInfoSnapshotConfigureEvent extends GoMintCloudNetEvent {

    private final ServiceInfoSnapshot serviceInfoSnapshot;

    public GoMintServiceInfoSnapshotConfigureEvent(ServiceInfoSnapshot serviceInfoSnapshot) {
        this.serviceInfoSnapshot = serviceInfoSnapshot;
    }

    public ServiceInfoSnapshot getServiceInfoSnapshot() {
        return this.serviceInfoSnapshot;
    }
}