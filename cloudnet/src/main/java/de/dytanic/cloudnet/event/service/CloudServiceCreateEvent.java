package de.dytanic.cloudnet.event.service;

import de.dytanic.cloudnet.driver.event.ICancelable;
import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@RequiredArgsConstructor
public final class CloudServiceCreateEvent extends DriverEvent implements ICancelable {

    private final ServiceConfiguration serviceConfiguration;

    @Setter
    private boolean cancelled;

}