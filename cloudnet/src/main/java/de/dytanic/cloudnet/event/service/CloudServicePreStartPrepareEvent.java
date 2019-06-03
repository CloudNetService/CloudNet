package de.dytanic.cloudnet.event.service;

import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.service.ICloudService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class CloudServicePreStartPrepareEvent extends DriverEvent {

    private final ICloudService cloudService;

}