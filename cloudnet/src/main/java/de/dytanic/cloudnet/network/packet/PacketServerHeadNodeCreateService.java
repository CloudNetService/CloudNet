package de.dytanic.cloudnet.network.packet;

import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;

public final class PacketServerHeadNodeCreateService extends Packet {

    public PacketServerHeadNodeCreateService(ServiceConfiguration serviceConfiguration) {
        super(PacketConstants.CLUSTER_NODE_CREATE_SERVICE_CHANNEL, ProtocolBuffer.create().writeObject(serviceConfiguration));
    }

}
