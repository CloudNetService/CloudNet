package de.dytanic.cloudnet.network.packet;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.network.NetworkUpdateType;

import java.util.Collection;

public final class PacketServerSetPermissionData extends Packet {

    public PacketServerSetPermissionData(Collection<IPermissionUser> permissionUsers, Collection<IPermissionGroup> permissionGroups, NetworkUpdateType updateType) {
        super(PacketConstants.INTERNAL_CLUSTER_CHANNEL, new JsonDocument()
                .append("permissionUsers", permissionUsers)
                .append("permissionGroups", permissionGroups)
                .append("updateType", updateType)
                .append("set_json_database", true), null);
    }

    public PacketServerSetPermissionData(Collection<IPermissionGroup> permissionGroups, NetworkUpdateType updateType, boolean ignored) {
        super(PacketConstants.INTERNAL_CLUSTER_CHANNEL, new JsonDocument()
                .append("permissionGroups", permissionGroups)
                .append("updateType", updateType)
                .append("set_json_database", true), null);
    }

    public PacketServerSetPermissionData(Collection<IPermissionUser> permissionUsers, NetworkUpdateType updateType) {
        super(PacketConstants.INTERNAL_CLUSTER_CHANNEL, new JsonDocument()
                .append("permissionUsers", permissionUsers)
                .append("updateType", updateType)
                .append("set_json_database", true), null);
    }
}