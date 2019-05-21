package de.dytanic.cloudnet.ext.bridge.sponge.event;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class SpongeCloudServiceRegisterEvent extends SpongeCloudNetEvent {

    @Getter
    private final ServiceInfoSnapshot serviceInfoSnapshot;
}