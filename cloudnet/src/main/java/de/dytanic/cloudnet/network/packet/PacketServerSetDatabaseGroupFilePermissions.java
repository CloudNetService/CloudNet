package de.dytanic.cloudnet.network.packet;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;

import java.util.Collection;

public final class PacketServerSetDatabaseGroupFilePermissions extends Packet {

    public PacketServerSetDatabaseGroupFilePermissions(Collection<IPermissionGroup> permissionGroups)
    {
        super(PacketConstants.INTERNAL_CLUSTER_CHANNEL, new JsonDocument()
            .append("permissionGroups", permissionGroups)
            .append("set_json_database", true), null);
    }
}