package de.dytanic.cloudnet.event.service;

import de.dytanic.cloudnet.driver.event.ICancelable;
import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.driver.service.ServiceDeployment;
import de.dytanic.cloudnet.service.ICloudService;
import de.dytanic.cloudnet.template.ITemplateStorage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@RequiredArgsConstructor
public final class CloudServiceDeploymentEvent extends DriverEvent implements ICancelable {

    private final ICloudService cloudService;

    private final ITemplateStorage templateStorage;

    private final ServiceDeployment serviceDeployment;

    @Setter
    private boolean cancelled;

}