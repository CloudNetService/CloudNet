package de.dytanic.cloudnet.ext.bridge.gomint.event;

import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class GoMintNetworkChannelPacketReceiveEvent extends GoMintCloudNetEvent {

    @Getter
    private final INetworkChannel channel;

    @Getter
    private final IPacket packet;
}