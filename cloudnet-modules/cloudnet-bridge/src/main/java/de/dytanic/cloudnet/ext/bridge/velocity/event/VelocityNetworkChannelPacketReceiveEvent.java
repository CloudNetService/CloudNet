package de.dytanic.cloudnet.ext.bridge.velocity.event;

import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;

public final class VelocityNetworkChannelPacketReceiveEvent extends VelocityCloudNetEvent {

    private final INetworkChannel channel;

    private final IPacket packet;

    public VelocityNetworkChannelPacketReceiveEvent(INetworkChannel channel, IPacket packet) {
        this.channel = channel;
        this.packet = packet;
    }

    public INetworkChannel getChannel() {
        return this.channel;
    }

    public IPacket getPacket() {
        return this.packet;
    }
}