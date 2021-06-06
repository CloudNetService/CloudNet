package de.dytanic.cloudnet.network.listener.cluster;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.event.network.NetworkClusterNodeInfoUpdateEvent;

public final class PacketServerClusterNodeInfoUpdateListener implements IPacketListener {

  @Override
  public void handle(INetworkChannel channel, IPacket packet) {
    NetworkClusterNodeInfoSnapshot snapshot = packet.getBuffer().readObject(NetworkClusterNodeInfoSnapshot.class);
    IClusterNodeServer clusterNodeServer = CloudNet.getInstance().getClusterNodeServerProvider()
      .getNodeServer(snapshot.getNode().getUniqueId());

    if (clusterNodeServer != null) {
      clusterNodeServer.setNodeInfoSnapshot(snapshot);
      CloudNetDriver.getInstance().getEventManager()
        .callEvent(new NetworkClusterNodeInfoUpdateEvent(channel, snapshot));
    }
  }

}
