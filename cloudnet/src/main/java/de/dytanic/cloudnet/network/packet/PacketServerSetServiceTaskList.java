package de.dytanic.cloudnet.network.packet;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.service.ServiceTask;

import java.util.Collection;

public final class PacketServerSetServiceTaskList extends Packet {

    public PacketServerSetServiceTaskList(Collection<ServiceTask> tasks)
    {
        super(PacketConstants.INTERNAL_CLUSTER_CHANNEL, new JsonDocument("taskList", tasks).append("set", true), new byte[0]);
    }
}