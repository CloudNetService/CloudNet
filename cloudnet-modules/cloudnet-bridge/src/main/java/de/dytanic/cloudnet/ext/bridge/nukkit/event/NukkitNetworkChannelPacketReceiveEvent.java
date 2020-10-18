package de.dytanic.cloudnet.ext.bridge.nukkit.event;

import cn.nukkit.event.HandlerList;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;

/**
 * {@inheritDoc}
 */
public final class NukkitNetworkChannelPacketReceiveEvent extends NukkitCloudNetEvent {

    private static final HandlerList handlers = new HandlerList();

    private final INetworkChannel channel;

    private final IPacket packet;

    public NukkitNetworkChannelPacketReceiveEvent(INetworkChannel channel, IPacket packet) {
        this.channel = channel;
        this.packet = packet;
    }

    public static HandlerList getHandlers() {
        return NukkitNetworkChannelPacketReceiveEvent.handlers;
    }

    public INetworkChannel getChannel() {
        return this.channel;
    }

    public IPacket getPacket() {
        return this.packet;
    }
}