package de.dytanic.cloudnet.network.listener;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.service.ICloudService;

public class PacketServerSetGlobalLogLevelListener implements IPacketListener {

    private boolean redirectToCluster;

    public PacketServerSetGlobalLogLevelListener(boolean redirectToCluster) {
        this.redirectToCluster = redirectToCluster;
    }

    @Override
    public void handle(INetworkChannel channel, IPacket packet) throws Exception {
        for (ICloudService localCloudService : CloudNet.getInstance().getCloudServiceManager().getLocalCloudServices()) {
            if (localCloudService.getNetworkChannel() != null) {
                localCloudService.getNetworkChannel().sendPacket(packet);
            }
        }
        if (this.redirectToCluster) {
            for (IClusterNodeServer nodeServer : CloudNet.getInstance().getClusterNodeServerProvider().getNodeServers()) {
                if (nodeServer.getChannel() != null) {
                    nodeServer.getChannel().sendPacket(packet);
                }
            }
        }

        CloudNetDriver.getInstance().getLogger().setLevel(packet.getHeader().getInt("level"));
    }

}
