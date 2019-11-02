package de.dytanic.cloudnet.network.packet;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.network.NetworkUpdateType;

import java.util.Collection;

public final class PacketServerSetGroupConfigurationList extends Packet {

    public PacketServerSetGroupConfigurationList(Collection<GroupConfiguration> groupConfigurations, NetworkUpdateType updateType) {
        super(PacketConstants.INTERNAL_CLUSTER_CHANNEL, new JsonDocument("groups", groupConfigurations).append("set", true).append("updateType", updateType), new byte[0]);
    }
}