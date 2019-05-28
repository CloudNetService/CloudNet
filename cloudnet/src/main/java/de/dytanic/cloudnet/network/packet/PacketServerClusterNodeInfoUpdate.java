package de.dytanic.cloudnet.network.packet;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;

public final class PacketServerClusterNodeInfoUpdate extends Packet {

  public PacketServerClusterNodeInfoUpdate(
      NetworkClusterNodeInfoSnapshot networkClusterNodeInfoSnapshot) {
    super(PacketConstants.INTERNAL_CLUSTER_CHANNEL,
        new JsonDocument("clusterNodeInfoSnapshot",
            networkClusterNodeInfoSnapshot), new byte[0]);
  }
}