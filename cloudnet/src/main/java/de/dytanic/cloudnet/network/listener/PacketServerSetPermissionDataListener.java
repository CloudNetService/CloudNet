package de.dytanic.cloudnet.network.listener;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import de.dytanic.cloudnet.network.NetworkUpdateType;
import de.dytanic.cloudnet.permission.ClusterSynchronizedPermissionManagement;

import java.util.List;

public final class PacketServerSetPermissionDataListener implements IPacketListener {

    @Override
    public void handle(INetworkChannel channel, IPacket packet) {
        if ((packet.getHeader().contains("permissionGroups") || packet.getHeader().contains("permissionUsers")) &&
                packet.getHeader().contains("set_json_database")) {
            if (CloudNet.getInstance().getPermissionManagement() instanceof ClusterSynchronizedPermissionManagement) {
                List<PermissionGroup> permissionGroups = packet.getHeader().get("permissionGroups", new TypeToken<List<PermissionGroup>>() {
                }.getType());
                NetworkUpdateType updateType = packet.getHeader().get("updateType", NetworkUpdateType.class);

                if (permissionGroups != null &&
                        CloudNet.getInstance().getPermissionManagement() instanceof ClusterSynchronizedPermissionManagement) {
                    ClusterSynchronizedPermissionManagement permissionManagement = (ClusterSynchronizedPermissionManagement) CloudNet.getInstance().getPermissionManagement();
                    switch (updateType) {
                        case SET:
                            permissionManagement.setGroupsWithoutClusterSyncAsync(permissionGroups);
                            break;
                        case ADD:
                            for (PermissionGroup permissionGroup : permissionGroups) {
                                permissionManagement.addGroupWithoutClusterSyncAsync(permissionGroup);
                            }
                            break;
                        case REMOVE:
                            for (PermissionGroup permissionGroup : permissionGroups) {
                                permissionManagement.deleteGroupWithoutClusterSyncAsync(permissionGroup);
                            }
                            break;
                    }
                }
            }
        }
    }
}