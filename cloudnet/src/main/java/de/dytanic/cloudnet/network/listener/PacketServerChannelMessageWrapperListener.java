package de.dytanic.cloudnet.network.listener;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerChannelMessage;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.network.protocol.PacketListener;

public final class PacketServerChannelMessageWrapperListener implements PacketListener {

    @Override
    public void handle(INetworkChannel channel, Packet packet) {
        if (packet.getHeader().contains("channel") && packet.getHeader().contains("message") && packet.getHeader().contains("data")) {
            if (!packet.getHeader().contains("uniqueId") && !packet.getHeader().contains("task")) { //only send to all nodes if the packet comes from a service
                Packet response = new PacketClientServerChannelMessage(
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