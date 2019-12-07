package de.dytanic.cloudnet.ext.bridge.velocity.event;

import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.Packet;

public final class VelocityNetworkChannelPacketReceiveEvent extends VelocityCloudNetEvent {

    private final INetworkChannel channel;

    private final Packet packet;

    public VelocityNetworkChannelPacketReceiveEvent(INetworkChannel channel, Packet packet) {
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