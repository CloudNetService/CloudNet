package de.dytanic.cloudnet.network.listener;

import de.dytanic.cloudnet.common.concurrent.CompletedTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.provider.CloudMessenger;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.provider.NodeMessenger;

import java.util.Collection;

public final class PacketServerChannelMessageListener implements IPacketListener {

    private final boolean redirectToCluster;

    public PacketServerChannelMessageListener(boolean redirectToCluster) {
        this.redirectToCluster = redirectToCluster;
    }

    @Override
    public void handle(INetworkChannel channel, IPacket packet) {
        ChannelMessage message = packet.getBuffer().readObject(ChannelMessage.class);
        boolean query = packet.getBuffer().readBoolean();

        CloudMessenger messenger = CloudNetDriver.getInstance().getMessenger();

        this.redirectMessage(messenger, message, query).onComplete(response -> {
            if (response != null) {
                channel.sendPacket(new Packet(-1, packet.getUniqueId(), JsonDocument.EMPTY, ProtocolBuffer.create().writeObjectCollection(response)));
            }
        });
    }

    private ITask<Collection<ChannelMessage>> redirectMessage(CloudMessenger messenger, ChannelMessage message, boolean query) {
        NodeMessenger nodeMessenger = (NodeMessenger) messenger;

        if (query) {
            return nodeMessenger.sendChannelMessageQueryAsync(message, !this.redirectToCluster);
        } else {
            nodeMessenger.sendChannelMessage(message, !this.redirectToCluster);
            return CompletedTask.create(null);
        }
    }

}