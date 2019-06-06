package de.dytanic.cloudnet.event.service;

import de.dytanic.cloudnet.driver.event.ICancelable;
import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.driver.service.ServiceDeployment;
import de.dytanic.cloudnet.service.ICloudService;
import de.dytanic.cloudnet.template.ITemplateStorage;

public final class CloudServiceDeploymentEvent extends DriverEvent implements ICancelable {

    private final ICloudService cloudService;

    private final ITemplateStorage templateStorage;

    private final ServiceDeployment serviceDeployment;

    private boolean cancelled;

    public CloudServiceDeploymentEvent(ICloudService cloudService, ITemplateStorage templateStorage, ServiceDeployment serviceDeployment) {
        this.cloudService = cloudService;
        this.templateStorage = templateStorage;
        this.serviceDeployment = serviceDeployment;
    }

    public ICloudService getCloudService() {
        return this.cloudService;
    }

    public ITemplateStorage getTemplateStorage() {
        return this.templateStorage;
    }

    public ServiceDeployment getServiceDeployment() {
        return this.serviceDeployment;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}