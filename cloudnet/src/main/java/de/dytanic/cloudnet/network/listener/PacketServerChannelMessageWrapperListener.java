package de.dytanic.cloudnet.network.listener;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerChannelMessage;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.service.ICloudService;

import java.util.UUID;

public final class PacketServerChannelMessageWrapperListener implements IPacketListener {

    @Override
    public void handle(INetworkChannel channel, IPacket packet) throws Exception {
        if (packet.getHeader().contains("channel") && packet.getHeader().contains("message") && packet.getHeader().contains("data")) {
            if (!packet.getHeader().contains("uniqueId") && !packet.getHeader().contains("task")) { //only send to all nodes if the packet comes from a service
                IPacket response = new PacketClientServerChannelMessage(
                        packet.getHeader().getString("channel"),
                        packet.getHeader().getString("message"),
                        packet.getHeader().getDocument("data")
                );
                for (IClusterNodeServer nodeServer : CloudNet.getInstance().getClusterNodeServerProvider().getNodeServers()) {
                    nodeServer.saveSendPacket(response);
                }
            }
        }
    }
}