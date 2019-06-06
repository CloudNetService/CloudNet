package de.dytanic.cloudnet.ext.bridge.bungee.event;

import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;

public final class BungeeNetworkChannelPacketReceiveEvent extends BungeeCloudNetEvent {

    private final INetworkChannel channel;

    private final IPacket packet;

    public BungeeNetworkChannelPacketReceiveEvent(INetworkChannel channel, IPacket packet) {
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