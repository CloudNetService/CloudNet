package de.dytanic.cloudnet.network.packet;

import de.dytanic.cloudnet.driver.network.NetworkUpdateType;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;

import java.util.Collection;

public final class PacketServerSetPermissionData extends Packet {

    public PacketServerSetPermissionData(Collection<IPermissionGroup> permissionGroups, NetworkUpdateType updateType) {
        super(PacketConstants.CLUSTER_PERMISSION_DATA_CHANNEL, ProtocolBuffer.create().writeObjectCollection(permissionGroups).writeEnumConstant(updateType));
    }
}