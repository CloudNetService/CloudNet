package de.dytanic.cloudnet.network.listener;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.event.cluster.NetworkClusterChannelMessageReceiveEvent;

public final class PacketServerClusterChannelMessageListener implements IPacketListener {

    @Override
    public void handle(INetworkChannel channel, IPacket packet) {
        ProtocolBuffer body = packet.getBody();

        CloudNetDriver.getInstance().getEventManager().callEvent(new NetworkClusterChannelMessageReceiveEvent(
                channel,
                body.readString(),
                body.readString(),
                JsonDocument.newDocument(body.readString()),
                body.readArray()
        ));
    }
}