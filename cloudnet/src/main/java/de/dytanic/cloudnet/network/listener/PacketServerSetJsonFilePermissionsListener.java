package de.dytanic.cloudnet.network.listener;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.permission.DefaultJsonFilePermissionManagement;
import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import de.dytanic.cloudnet.event.network.NetworkChannelReceiveJsonFilePermissionsUpdateEvent;

import java.util.List;

public final class PacketServerSetJsonFilePermissionsListener implements IPacketListener {

    @Override
    public void handle(INetworkChannel channel, IPacket packet) throws Exception {
        if (packet.getHeader().contains("permissionUsers") && packet.getHeader().contains("permissionGroups") && packet.getHeader().contains("set_json_file")) {
            if (CloudNet.getInstance().getPermissionManagement() instanceof DefaultJsonFilePermissionManagement) {
                List<PermissionUser> permissionUsers = packet.getHeader().get("permissionUsers", new TypeToken<List<PermissionUser>>() {
                }.getType());

                List<PermissionGroup> permissionGroups = packet.getHeader().get("permissionGroups", new TypeToken<List<PermissionGroup>>() {
                }.getType());

                if (permissionUsers != null || permissionGroups != null) {
                    NetworkChannelReceiveJsonFilePermissionsUpdateEvent event = new NetworkChannelReceiveJsonFilePermissionsUpdateEvent(channel, permissionUsers, permissionGroups);
                    CloudNetDriver.getInstance().getEventManager().callEvent(event);

                    if (!event.isCancelled()) {
                        if (permissionUsers != null) {
                            ((DefaultJsonFilePermissionManagement) CloudNet.getInstance().getPermissionManagement()).setUsers0(
                                    event.getPermissionUsers() != null ? event.getPermissionUsers() : permissionUsers
                            );
                        }

                        if (permissionGroups != null) {
                            ((DefaultJsonFilePermissionManagement) CloudNet.getInstance().getPermissionManagement()).setGroups0(
                                    event.getPermissionGroups() != null ? event.getPermissionGroups() : permissionGroups
                            );
                        }
                    }
                }
            }
        }
    }
}