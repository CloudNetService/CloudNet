package de.dytanic.cloudnet.driver.network.def.packet;

import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

public final class PacketClientServerServiceInfoPublisher extends Packet {

    public PacketClientServerServiceInfoPublisher(ServiceInfoSnapshot serviceInfoSnapshot, PublisherType publisherType) {
        super(PacketConstants.SERVICE_INFO_PUBLISH_CHANNEL, ProtocolBuffer.create().writeObject(serviceInfoSnapshot).writeEnumConstant(publisherType));
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