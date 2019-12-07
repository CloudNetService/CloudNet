package de.dytanic.cloudnet.ext.bridge.gomint.event;

import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.Packet;

public final class GoMintNetworkChannelPacketReceiveEvent extends GoMintCloudNetEvent {

    private final INetworkChannel channel;

    private final Packet packet;

    public GoMintNetworkChannelPacketReceiveEvent(INetworkChannel channel, Packet packet) {
        this.channel = channel;
        this.packet = packet;
    }

    public INetworkChannel getChannel() {
        return this.channel;
    }

    public Packet getPacket() {
        return this.packet;
    }
}