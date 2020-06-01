package de.dytanic.cloudnet.network.listener;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerChannelMessage;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.provider.CloudMessenger;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.provider.NodeMessenger;

import java.util.ArrayList;
import java.util.Collection;

public final class PacketServerChannelMessageListener implements IPacketListener {

    private final boolean redirectToCluster;

    public PacketServerChannelMessageListener(boolean redirectToCluster) {
        this.redirectToCluster = redirectToCluster;
    }

    @Override
    public void handle(INetworkChannel channel, IPacket packet) {
        ChannelMessage message = packet.getBody().readObject(ChannelMessage.class);
        boolean query = packet.getBody().readBoolean();

        CloudMessenger messenger = CloudNetDriver.getInstance().getMessenger();

        Collection<ChannelMessage> response = null;

        if (this.redirectToCluster) {

            if (query) {
                response = messenger.sendChannelMessageQuery(message);
            } else {
                messenger.sendChannelMessage(message);
            }

        } else if (messenger instanceof NodeMessenger) {
            Collection<INetworkChannel> channels = ((NodeMessenger) messenger).getTargetChannels(message.getTarget(), true);

            if (channels != null && !channels.isEmpty()) {
                if (query) {
                    response = new ArrayList<>();
                }

                IPacket clientPacket = new PacketClientServerChannelMessage(message, query);
                for (INetworkChannel targetChannel : channels) {
                    targetChannel.sendPacket(clientPacket);
                    // TODO query
                }
            }
        }

        if (response != null) {
            channel.sendPacket(new Packet(-1, packet.getUniqueId(), JsonDocument.EMPTY, ProtocolBuffer.create().writeObjectCollection(response)));
        }
    }
}