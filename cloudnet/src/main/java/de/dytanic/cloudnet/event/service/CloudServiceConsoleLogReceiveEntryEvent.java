package de.dytanic.cloudnet.event.service;

import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class CloudServiceConsoleLogReceiveEntryEvent extends DriverEvent {

    private final ServiceInfoSnapshot serviceInfoSnapshot;

    private final String message;

    private final boolean errorMessage;

}