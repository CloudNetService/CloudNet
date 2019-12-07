package de.dytanic.cloudnet.driver.network.def.packet;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.AbstractPacket;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

public final class PacketClientServerServiceInfoPublisher extends AbstractPacket {

    public PacketClientServerServiceInfoPublisher(ServiceInfoSnapshot serviceInfoSnapshot, PublisherType publisherType) {
        super(PacketConstants.INTERNAL_EVENTBUS_CHANNEL,
                new JsonDocument()
                        .append("serviceInfoSnapshot", serviceInfoSnapshot)
                        .append("type", publisherType)
                ,
                null
        );
    }

    public enum PublisherType {
        UPDATE,
        STARTED,
        STOPPED,
        CONNECTED,
        DISCONNECTED,
        UNREGISTER,
        REGISTER
    }
}