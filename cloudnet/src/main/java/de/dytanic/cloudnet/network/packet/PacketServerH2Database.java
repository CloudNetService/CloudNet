package de.dytanic.cloudnet.network.packet;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.AbstractPacket;

public final class PacketServerH2Database extends AbstractPacket {

    public PacketServerH2Database(OperationType operationType, String name, String key, JsonDocument document) {
        super(PacketConstants.INTERNAL_H2_DATABASE_UPDATE_MODULE, new JsonDocument("operationType", operationType)
                        .append("name", name)
                        .append("key", key)
                        .append("document", document),
                new byte[0]);
    }

    public enum OperationType {
        INSERT,
        UPDATE,
        DELETE,
        CLEAR
    }
}