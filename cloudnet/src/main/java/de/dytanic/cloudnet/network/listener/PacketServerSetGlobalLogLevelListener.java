package de.dytanic.cloudnet.network.listener;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.packet.PacketServerSetGlobalLogLevel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.service.ICloudService;

public class PacketServerSetGlobalLogLevelListener implements IPacketListener {

  private final boolean redirectToCluster;

  public PacketServerSetGlobalLogLevelListener(boolean redirectToCluster) {
    this.redirectToCluster = redirectToCluster;
  }

  @Override
  public void handle(INetworkChannel channel, IPacket packet) {
    int level = packet.getBuffer().readInt();
    packet = new PacketServerSetGlobalLogLevel(level);

    CloudNetDriver.getInstance().getLogger().setLevel(level);

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
  }

}
