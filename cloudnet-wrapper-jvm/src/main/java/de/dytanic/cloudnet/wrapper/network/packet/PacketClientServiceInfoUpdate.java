package de.dytanic.cloudnet.wrapper.network.packet;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

public final class PacketClientServiceInfoUpdate extends Packet {

    public PacketClientServiceInfoUpdate(ServiceInfoSnapshot serviceInfoSnapshot) {
        super(PacketConstants.INTERNAL_WRAPPER_TO_NODE_INFO_CHANNEL, new JsonDocument("message", "update_serviceInfo").append("serviceInfoSnapshot", serviceInfoSnapshot), Packet.EMPTY_PACKET_BYTE_ARRAY);
    }
}