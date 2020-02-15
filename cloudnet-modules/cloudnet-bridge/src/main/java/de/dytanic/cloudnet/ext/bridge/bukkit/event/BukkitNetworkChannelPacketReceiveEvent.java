package de.dytanic.cloudnet.ext.bridge.bukkit.event;

import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class BukkitNetworkChannelPacketReceiveEvent extends BukkitCloudNetEvent {

    private static HandlerList handlerList = new HandlerList();

    private final INetworkChannel channel;

    private final IPacket packet;

    public BukkitNetworkChannelPacketReceiveEvent(INetworkChannel channel, IPacket packet) {
        this.channel = channel;
        this.packet = packet;
    }

    public static HandlerList getHandlerList() {
        return BukkitNetworkChannelPacketReceiveEvent.handlerList;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public INetworkChannel getChannel() {
        return this.channel;
    }

    public IPacket getPacket() {
        return this.packet;
    }
}