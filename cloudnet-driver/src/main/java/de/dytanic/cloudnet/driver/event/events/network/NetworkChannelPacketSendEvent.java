package de.dytanic.cloudnet.driver.event.events.network;

import de.dytanic.cloudnet.driver.event.ICancelable;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;

public class NetworkChannelPacketSendEvent extends NetworkEvent implements ICancelable {

    private final IPacket packet;
    private boolean cancelled;

    public NetworkChannelPacketSendEvent(INetworkChannel channel, IPacket packet) {
        super(channel);
        this.packet = packet;
    }

    public IPacket getPacket() {
        return this.packet;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

}
