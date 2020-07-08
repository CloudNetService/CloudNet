package de.dytanic.cloudnet.network.listener;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.channel.ChannelMessageTarget;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
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
        ChannelMessage message = packet.getBuffer().readObject(ChannelMessage.class);
        boolean query = packet.getBuffer().readBoolean();

        CloudMessenger messenger = CloudNetDriver.getInstance().getMessenger();

        Collection<ChannelMessage> response = query ? new ArrayList<>() : null;

        this.handleMessage(messenger, response, message, query);

        if (response != null) {
            channel.sendPacket(new Packet(-1, packet.getUniqueId(), JsonDocument.EMPTY, ProtocolBuffer.create().writeObjectCollection(response)));
        }
    }

    private void handleMessage(CloudMessenger messenger, Collection<ChannelMessage> response, ChannelMessage message, boolean query) {
        for (ChannelMessageTarget target : message.getTargets()) {
            if (this.hasReceived(target)) {
                this.fireEvent(message, response, query);
                break;
            }
        }


        if (this.redirectToCluster) {

            if (query) {
                response.addAll(messenger.sendChannelMessageQuery(message));
            } else {
                messenger.sendChannelMessage(message);
            }

        } else if (messenger instanceof NodeMessenger) {
            for (ChannelMessageTarget target : message.getTargets()) {
                Collection<INetworkChannel> channels = ((NodeMessenger) messenger).getTargetChannels(message.getSender(), target, true);

                if (channels != null && !channels.isEmpty()) {
                    IPacket clientPacket = new PacketClientServerChannelMessage(message, query);
                    for (INetworkChannel targetChannel : channels) {
                        if (query) {
                            IPacket queryResponse = targetChannel.sendQuery(clientPacket);
                            if (queryResponse != null && queryResponse.getBuffer().readBoolean()) {
                                response.add(queryResponse.getBuffer().readObject(ChannelMessage.class));
                            }
                        } else {
                            targetChannel.sendPacket(clientPacket);
                        }
                    }
                }
            }
        }
    }

    private void fireEvent(ChannelMessage message, Collection<ChannelMessage> response, boolean query) {
        message.getBuffer().markReaderIndex();

        ChannelMessageReceiveEvent event = new ChannelMessageReceiveEvent(message, query);
        CloudNetDriver.getInstance().getEventManager().callEvent(event);

        if (event.getQueryResponse() != null && query) {
            response.add(event.getQueryResponse());
        }

        message.getBuffer().resetReaderIndex();
    }

    private boolean hasReceived(ChannelMessageTarget target) {
        ChannelMessageTarget.Type targetType = target.getType();
        if (targetType.equals(ChannelMessageTarget.Type.NODE)) {
            return target.getName() == null || CloudNetDriver.getInstance().getComponentName().equals(target.getName());
        }
        return targetType.equals(ChannelMessageTarget.Type.ALL);
    }

}