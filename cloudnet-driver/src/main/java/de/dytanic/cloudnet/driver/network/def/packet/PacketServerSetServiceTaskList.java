package de.dytanic.cloudnet.driver.network.def.packet;

import de.dytanic.cloudnet.driver.network.NetworkUpdateType;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceTask;

import java.util.Collection;

public final class PacketServerSetServiceTaskList extends Packet {

    public PacketServerSetServiceTaskList(Collection<ServiceTask> tasks, NetworkUpdateType updateType) {
        super(PacketConstants.CLUSTER_TASK_LIST_CHANNEL, ProtocolBuffer.create().writeObjectCollection(tasks).writeEnumConstant(updateType));
    }

}