package de.dytanic.cloudnet.network.packet;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

import java.util.Collection;

public final class PacketServerSetGlobalServiceInfoList extends Packet {

    public PacketServerSetGlobalServiceInfoList(Collection<ServiceInfoSnapshot> serviceInfoSnapshots) {
        super(PacketConstants.INTERNAL_CLUSTER_CHANNEL, new JsonDocument("serviceInfoList", serviceInfoSnapshots).append("set", true), new byte[]{0});
    }

}