package de.dytanic.cloudnet.event.service;

import de.dytanic.cloudnet.driver.event.ICancelable;
import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.driver.service.ServiceRemoteInclusion;
import de.dytanic.cloudnet.service.ICloudService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.net.URLConnection;

@Getter
@RequiredArgsConstructor
public final class CloudServicePreLoadInclusionEvent extends DriverEvent implements ICancelable {

    private final ICloudService cloudService;

    private final ServiceRemoteInclusion serviceRemoteInclusion;

    private final URLConnection connection;

    @Setter
    private boolean cancelled;

}