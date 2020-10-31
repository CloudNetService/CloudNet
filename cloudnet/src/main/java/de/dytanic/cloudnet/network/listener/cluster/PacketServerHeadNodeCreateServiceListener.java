package de.dytanic.cloudnet.network.listener.cluster;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

public final class PacketServerHeadNodeCreateServiceListener implements IPacketListener {

    private final CloudNet cloudNet;

    public PacketServerHeadNodeCreateServiceListener(CloudNet cloudNet) {
        this.cloudNet = cloudNet;
    }

    @Override
    public void handle(INetworkChannel channel, IPacket packet) {
        IClusterNodeServer server = this.cloudNet.getClusterNodeServerProvider().getNodeServer(channel);
        if (server == null) {
            packet.getBuffer().skipBytes(packet.getBuffer().readableBytes());
            channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeOptionalObject(null)));
            return;
        }

        ServiceConfiguration serviceConfiguration = packet.getBuffer().readObject(ServiceConfiguration.class);

        if (this.cloudNet.getClusterNodeServerProvider().getHeadNode().isInstance(server)) {
            this.sendResult(channel, packet, this.cloudNet.getCloudServiceManager().buildService(serviceConfiguration));
        } else {
            this.sendResult(channel, packet, this.cloudNet.getCloudServiceFactory().createCloudServiceAsync(serviceConfiguration));
        }
    }

    private void sendResult(INetworkChannel channel, IPacket packet, ITask<ServiceInfoSnapshot> task) {
        task
                .onFailure(ignored -> channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeOptionalObject(null))))
                .onComplete(info -> channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeOptionalObject(info))))
                .onCancelled(ignored -> channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeOptionalObject(null))));
    }

}
