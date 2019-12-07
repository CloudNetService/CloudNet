package de.dytanic.cloudnet.network.packet;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.AbstractPacket;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

public final class PacketServerConsoleLogEntryReceive extends AbstractPacket {

    public PacketServerConsoleLogEntryReceive(ServiceInfoSnapshot serviceInfoSnapshot, String message, boolean errorMessage) {
        super(PacketConstants.INTERNAL_CLUSTER_CHANNEL, new JsonDocument("command", "log_entry_receive")
                        .append("serviceInfoSnapshot", serviceInfoSnapshot)
                        .append("messageEntry", message)
                        .append("errorMessage", errorMessage),
                new byte[0]);
    }
}