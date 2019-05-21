package de.dytanic.cloudnet.event.network;

import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.service.ICloudService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class NetworkChannelAuthCloudServiceSuccessEvent extends Event {

    private final ICloudService cloudService;

    private final INetworkChannel channel;

}