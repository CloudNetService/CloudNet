package de.dytanic.cloudnet.network.packet;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;

import java.util.Collection;

public final class PacketServerSetJsonFilePermissions extends Packet {

    public PacketServerSetJsonFilePermissions(Collection<IPermissionUser> permissionUsers, Collection<IPermissionGroup> permissionGroups) {
        super(PacketConstants.INTERNAL_CLUSTER_CHANNEL, new JsonDocument()
                .append("permissionUsers", permissionUsers)
                .append("permissionGroups", permissionGroups)
                .append("set_json_file", true), null);
    }
}