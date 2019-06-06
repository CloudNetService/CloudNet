package de.dytanic.cloudnet.event.service;

import de.dytanic.cloudnet.driver.event.ICancelable;
import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;

public final class CloudServiceCreateEvent extends DriverEvent implements ICancelable {

    private final ServiceConfiguration serviceConfiguration;

    private boolean cancelled;

    public CloudServiceCreateEvent(ServiceConfiguration serviceConfiguration) {
        this.serviceConfiguration = serviceConfiguration;
    }

    public ServiceConfiguration getServiceConfiguration() {
        return this.serviceConfiguration;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}