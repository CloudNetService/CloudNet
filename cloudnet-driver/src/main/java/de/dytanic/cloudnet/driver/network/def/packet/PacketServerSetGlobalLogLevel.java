package de.dytanic.cloudnet.driver.network.def.packet;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;

public class PacketServerSetGlobalLogLevel extends Packet {

    public PacketServerSetGlobalLogLevel(int logLevel) {
        super(PacketConstants.INTERNAL_DEBUGGING_CHANNEL, new JsonDocument().append("level", logLevel));
    }

}
