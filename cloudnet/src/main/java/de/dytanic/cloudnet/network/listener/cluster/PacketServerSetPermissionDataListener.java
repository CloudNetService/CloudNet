package de.dytanic.cloudnet.network.listener.cluster;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import de.dytanic.cloudnet.network.NetworkUpdateType;
import de.dytanic.cloudnet.permission.ClusterSynchronizedPermissionManagement;
import java.util.Collection;

public final class PacketServerSetPermissionDataListener implements IPacketListener {

  @Override
  public void handle(INetworkChannel channel, IPacket packet) {
    if (CloudNet.getInstance().getPermissionManagement() instanceof ClusterSynchronizedPermissionManagement) {
      ClusterSynchronizedPermissionManagement permissionManagement = (ClusterSynchronizedPermissionManagement) CloudNet
        .getInstance().getPermissionManagement();

      Collection<PermissionGroup> permissionGroups = packet.getBuffer().readObjectCollection(PermissionGroup.class);
      NetworkUpdateType updateType = packet.getBuffer().readEnumConstant(NetworkUpdateType.class);

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
