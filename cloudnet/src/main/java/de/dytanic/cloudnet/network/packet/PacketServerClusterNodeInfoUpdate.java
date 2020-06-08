package de.dytanic.cloudnet.network.packet;

import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;

public final class PacketServerClusterNodeInfoUpdate extends Packet {

    public PacketServerClusterNodeInfoUpdate(NetworkClusterNodeInfoSnapshot networkClusterNodeInfoSnapshot) {
        super(PacketConstants.CLUSTER_NODE_INFO_CHANNEL, ProtocolBuffer.create().writeObject(networkClusterNodeInfoSnapshot));
    }

    @Override
    public boolean isShowDebug() {
        return false;
    }

}