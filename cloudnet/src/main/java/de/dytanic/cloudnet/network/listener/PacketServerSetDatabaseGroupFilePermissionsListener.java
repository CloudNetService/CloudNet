package de.dytanic.cloudnet.network.listener;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import de.dytanic.cloudnet.permission.DefaultDatabasePermissionManagement;
import java.util.List;

public final class PacketServerSetDatabaseGroupFilePermissionsListener implements
  IPacketListener {

  @Override
  public void handle(INetworkChannel channel, IPacket packet) throws Exception {
    if (packet.getHeader().contains("permissionGroups") && packet.getHeader()
      .contains("set_json_database")) {
      if (CloudNet.getInstance()
        .getPermissionManagement() instanceof DefaultDatabasePermissionManagement) {
        List<PermissionGroup> permissionGroups = packet.getHeader()
          .get("permissionGroups", new TypeToken<List<PermissionGroup>>() {
          }.getType());

        if (permissionGroups != null) {
          if (permissionGroups != null) {
            ((DefaultDatabasePermissionManagement) CloudNet.getInstance()
              .getPermissionManagement()).setGroups0(permissionGroups);
          }
        }
      }
    }
  }
}