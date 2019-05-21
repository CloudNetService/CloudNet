package de.dytanic.cloudnet.event.cluster;

import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class NetworkChannelAuthClusterNodeSuccessEvent extends Event {

    private final IClusterNodeServer node;

    private final INetworkChannel channel;

}