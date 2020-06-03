package de.dytanic.cloudnet.driver.network.def.packet;

import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;

import java.util.function.Consumer;

public class PacketClientDriverAPI extends Packet {

    public PacketClientDriverAPI(DriverAPIRequestType type) {
        this(type, null);
    }

    public PacketClientDriverAPI(DriverAPIRequestType type, Consumer<ProtocolBuffer> modifier) {
        super(PacketConstants.INTERNAL_DRIVER_API_CHANNEL, ProtocolBuffer.create().writeEnumConstant(type));
        if (modifier != null) {
            modifier.accept(super.body);
        }
    }

}
