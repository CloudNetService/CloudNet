package de.dytanic.cloudnet.event.service;

import de.dytanic.cloudnet.driver.event.ICancelable;
import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import de.dytanic.cloudnet.service.ICloudService;

public final class CloudServiceTemplateLoadEvent extends DriverEvent implements ICancelable {

    private final ICloudService cloudService;

    private final TemplateStorage storage;

    private final ServiceTemplate template;

    private boolean cancelled;

    public CloudServiceTemplateLoadEvent(ICloudService cloudService, TemplateStorage storage, ServiceTemplate template) {
        this.cloudService = cloudService;
        this.storage = storage;
        this.template = template;
    }

    public ICloudService getCloudService() {
        return this.cloudService;
    }

    public TemplateStorage getStorage() {
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