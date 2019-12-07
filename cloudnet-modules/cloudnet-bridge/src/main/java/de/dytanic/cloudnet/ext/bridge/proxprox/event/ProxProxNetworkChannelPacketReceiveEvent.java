package de.dytanic.cloudnet.ext.bridge.proxprox.event;

import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.Packet;

public final class ProxProxNetworkChannelPacketReceiveEvent extends ProxProxCloudNetEvent {

    private final INetworkChannel channel;

    private final Packet packet;

    public ProxProxNetworkChannelPacketReceiveEvent(INetworkChannel channel, Packet packet) {
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