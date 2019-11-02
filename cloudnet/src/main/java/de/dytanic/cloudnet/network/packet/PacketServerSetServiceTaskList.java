package de.dytanic.cloudnet.network.packet;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.network.NetworkUpdateType;

import java.util.Collection;

public final class PacketServerSetServiceTaskList extends Packet {

    public PacketServerSetServiceTaskList(Collection<ServiceTask> tasks, NetworkUpdateType updateType) {
        super(PacketConstants.INTERNAL_CLUSTER_CHANNEL, new JsonDocument("taskList", tasks).append("set", true).append("updateType", updateType), new byte[0]);
    }
}