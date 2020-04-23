package de.dytanic.cloudnet.event.service;

import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

/**
 * This event is called whenever a new console line is read from a service on this node,
 * it won't be called in the cluster.
 */
public final class CloudServiceConsoleLogReceiveEntryEvent extends DriverEvent {

    private final ServiceInfoSnapshot serviceInfoSnapshot;

    private final String message;

    private final boolean errorMessage;

    public CloudServiceConsoleLogReceiveEntryEvent(ServiceInfoSnapshot serviceInfoSnapshot, String message, boolean errorMessage) {
        this.serviceInfoSnapshot = serviceInfoSnapshot;
        this.message = message;
        this.errorMessage = errorMessage;
    }

    @Override
    public boolean isShowDebug() {
        return false;
    }

    public ServiceInfoSnapshot getServiceInfoSnapshot() {
        return this.serviceInfoSnapshot;
    }

    public String getMessage() {
        return this.message;
    }

    public boolean isErrorMessage() {
        return this.errorMessage;
    }

}