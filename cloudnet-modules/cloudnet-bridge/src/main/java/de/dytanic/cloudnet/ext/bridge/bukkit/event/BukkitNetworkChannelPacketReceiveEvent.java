package de.dytanic.cloudnet.ext.bridge.bukkit.event;

import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import org.bukkit.event.HandlerList;

public final class BukkitNetworkChannelPacketReceiveEvent extends BukkitCloudNetEvent {

    private static HandlerList handlerList = new HandlerList();

    private final INetworkChannel channel;

    private final Packet packet;

    public BukkitNetworkChannelPacketReceiveEvent(INetworkChannel channel, Packet packet) {
        this.channel = channel;
        this.packet = packet;
    }

    public static HandlerList getHandlerList() {
        return BukkitNetworkChannelPacketReceiveEvent.handlerList;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public INetworkChannel getChannel() {
        return this.channel;
    }

    public Packet getPacket() {
        return this.packet;
    }
}