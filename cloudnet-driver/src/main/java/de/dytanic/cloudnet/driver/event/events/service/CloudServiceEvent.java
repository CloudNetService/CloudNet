package de.dytanic.cloudnet.driver.event.events.service;

import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public abstract class CloudServiceEvent extends DriverEvent {

    private final ServiceInfoSnapshot serviceInfo;

}