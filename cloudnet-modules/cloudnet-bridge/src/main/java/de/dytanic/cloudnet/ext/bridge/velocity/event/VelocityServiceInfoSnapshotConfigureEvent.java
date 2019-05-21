package de.dytanic.cloudnet.ext.bridge.velocity.event;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class VelocityServiceInfoSnapshotConfigureEvent extends VelocityCloudNetEvent {

    @Getter
    private final ServiceInfoSnapshot serviceInfoSnapshot;
}