package de.dytanic.cloudnet.network.packet;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.AbstractPacket;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;

public final class PacketServerDeployLocalTemplate extends AbstractPacket {

    public PacketServerDeployLocalTemplate(ServiceTemplate template, byte[] data, boolean preClear) {
        super(PacketConstants.INTERNAL_CLUSTER_CHANNEL, new JsonDocument("command", "deploy_template").append("serviceTemplate", template).append("preClear", preClear), data);
    }
}