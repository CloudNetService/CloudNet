package de.dytanic.cloudnet.event.service;

import de.dytanic.cloudnet.driver.event.ICancelable;
import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.service.ICloudService;
import de.dytanic.cloudnet.template.ITemplateStorage;

public final class CloudServiceTemplateLoadEvent extends DriverEvent implements ICancelable {

    private final ICloudService cloudService;

    private final ITemplateStorage storage;

    private final ServiceTemplate template;

    private boolean cancelled;

    public CloudServiceTemplateLoadEvent(ICloudService cloudService, ITemplateStorage storage, ServiceTemplate template) {
        this.cloudService = cloudService;
        this.storage = storage;
        this.template = template;
    }

    public ICloudService getCloudService() {
        return this.cloudService;
    }

    public ITemplateStorage getStorage() {
        return this.storage;
    }

    public ServiceTemplate getTemplate() {
        return this.template;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}