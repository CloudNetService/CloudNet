package de.dytanic.cloudnet.driver.event.events.network;

import de.dytanic.cloudnet.driver.event.ICancelable;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.Packet;

public class NetworkChannelPacketReceiveEvent extends NetworkEvent implements ICancelable {

    private final Packet packet;
    private boolean cancelled;

    public NetworkChannelPacketReceiveEvent(INetworkChannel channel, Packet packet) {
        super(channel);
        this.packet = packet;
    }

    public Packet getPacket() {
        return this.packet;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}