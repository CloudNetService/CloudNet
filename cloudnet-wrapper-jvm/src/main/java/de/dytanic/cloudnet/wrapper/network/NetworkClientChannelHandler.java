package de.dytanic.cloudnet.wrapper.network;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.events.network.ChannelType;
import de.dytanic.cloudnet.driver.event.events.network.NetworkChannelCloseEvent;
import de.dytanic.cloudnet.driver.event.events.network.NetworkChannelInitEvent;
import de.dytanic.cloudnet.driver.event.events.network.NetworkChannelPacketReceiveEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.INetworkChannelHandler;
import de.dytanic.cloudnet.driver.network.def.internal.InternalSyncPacketChannel;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientAuthorization;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.wrapper.Wrapper;

public class NetworkClientChannelHandler implements INetworkChannelHandler {

    @Override
    public void handleChannelInitialize(INetworkChannel channel) {
        NetworkChannelInitEvent networkChannelInitEvent = new NetworkChannelInitEvent(channel, ChannelType.SERVER_CHANNEL);
        CloudNetDriver.getInstance().getEventManager().callEvent(networkChannelInitEvent);

        if (networkChannelInitEvent.isCancelled()) {
            try {
                channel.close();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return;
        }

        networkChannelInitEvent.getChannel().sendPacket(new PacketClientAuthorization(
                PacketClientAuthorization.PacketAuthorizationType.WRAPPER_TO_NODE,
                new JsonDocument()
                        .append("connectionKey", Wrapper.getInstance().getConfig().getConnectionKey())
                        .append("serviceId", Wrapper.getInstance().getConfig().getServiceConfiguration().getServiceId())
        ));
    }

    @Override
    public boolean handlePacketReceive(INetworkChannel channel, Packet packet) {
        if (InternalSyncPacketChannel.handleIncomingChannel(channel, packet)) {
            return false;
        }

        return !CloudNetDriver.getInstance().getEventManager().callEvent(new NetworkChannelPacketReceiveEvent(channel, packet)).isCancelled();
    }

    @Override
    public void handleChannelClose(INetworkChannel channel) {
        CloudNetDriver.getInstance().getEventManager().callEvent(new NetworkChannelCloseEvent(channel, ChannelType.CLIENT_CHANNEL));
    }
}