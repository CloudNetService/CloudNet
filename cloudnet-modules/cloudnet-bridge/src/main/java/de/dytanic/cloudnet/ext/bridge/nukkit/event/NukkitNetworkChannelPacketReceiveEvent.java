package de.dytanic.cloudnet.ext.bridge.nukkit.event;

import cn.nukkit.event.HandlerList;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.Packet;

public final class NukkitNetworkChannelPacketReceiveEvent extends NukkitCloudNetEvent {

    private static final HandlerList handlers = new HandlerList();

    private final INetworkChannel channel;

    private final Packet packet;

    public NukkitNetworkChannelPacketReceiveEvent(INetworkChannel channel, Packet packet) {
        this.channel = channel;
        this.packet = packet;
    }

    public static HandlerList getHandlers() {
        return NukkitNetworkChannelPacketReceiveEvent.handlers;
    }

    public INetworkChannel getChannel() {
        return this.channel;
    }

    public Packet getPacket() {
        return this.packet;
    }
}