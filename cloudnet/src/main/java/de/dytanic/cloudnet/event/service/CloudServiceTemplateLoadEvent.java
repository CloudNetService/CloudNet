package de.dytanic.cloudnet.event.service;

import de.dytanic.cloudnet.driver.event.ICancelable;
import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.service.ICloudService;
import de.dytanic.cloudnet.template.ITemplateStorage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@RequiredArgsConstructor
public final class CloudServiceTemplateLoadEvent extends DriverEvent implements ICancelable {

    private final ICloudService cloudService;

    private final ITemplateStorage storage;

    private final ServiceTemplate template;

    @Setter
    private boolean cancelled;

}