package de.dytanic.cloudnet.network.packet;

import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;

public final class PacketServerDeployLocalTemplate extends Packet {

    public PacketServerDeployLocalTemplate(ServiceTemplate template, byte[] data, boolean preClear) {
        super(PacketConstants.CLUSTER_TEMPLATE_DEPLOY_CHANNEL, ProtocolBuffer.create().writeObject(template).writeBoolean(preClear).writeArray(data));
    }
}