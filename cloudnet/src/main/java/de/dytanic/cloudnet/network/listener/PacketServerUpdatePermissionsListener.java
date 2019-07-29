package de.dytanic.cloudnet.network.listener;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.event.events.permission.*;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.packet.PacketServerUpdatePermissions;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import de.dytanic.cloudnet.service.ICloudService;

import java.lang.reflect.Type;
import java.util.Collection;

public final class PacketServerUpdatePermissionsListener implements IPacketListener {

    private static final Type PERMISSION_USERS_TYPE = new TypeToken<Collection<PermissionUser>>() {
    }.getType(), PERMISSION_GROUPS_TYPE = new TypeToken<Collection<PermissionGroup>>() {
    }.getType();

    @Override
    public void handle(INetworkChannel channel, IPacket packet) throws Exception {
        if (packet.getHeader().contains("permissions_event") && packet.getHeader().contains("updateType")) {
            switch (packet.getHeader().get("updateType", PacketServerUpdatePermissions.UpdateType.class)) {
                case ADD_USER:
                    invoke0(new PermissionAddUserEvent(getPermissionManagement(), packet.getHeader().get("permissionUser", PermissionUser.TYPE)));
                    break;
                case ADD_GROUP:
                    invoke0(new PermissionAddGroupEvent(getPermissionManagement(), packet.getHeader().get("permissionGroup", PermissionGroup.TYPE)));
                    break;
                case SET_USERS:
                    invoke0(new PermissionSetUsersEvent(getPermissionManagement(), packet.getHeader().get("permissionUsers", PERMISSION_USERS_TYPE)));
                    break;
                case SET_GROUPS:
                    invoke0(new PermissionSetGroupsEvent(getPermissionManagement(), packet.getHeader().get("permissionGroups", PERMISSION_GROUPS_TYPE)));
                    break;
                case DELETE_USER:
                    invoke0(new PermissionUpdateUserEvent(getPermissionManagement(), packet.getHeader().get("permissionUser", PermissionUser.TYPE)));
                    break;
                case UPDATE_USER:
                    invoke0(new PermissionDeleteUserEvent(getPermissionManagement(), packet.getHeader().get("permissionUser", PermissionUser.TYPE)));
                    break;
                case DELETE_GROUP:
                    invoke0(new PermissionDeleteGroupEvent(getPermissionManagement(), packet.getHeader().get("permissionGroup", PermissionGroup.TYPE)));
                    break;
                case UPDATE_GROUP:
                    invoke0(new PermissionUpdateGroupEvent(getPermissionManagement(), packet.getHeader().get("permissionGroup", PermissionGroup.TYPE)));
                    break;
            }

            sendUpdateToAllServices(packet);
        }
    }

    private void invoke0(Event event) {
        CloudNetDriver.getInstance().getEventManager().callEvent(event);
    }

    private IPermissionManagement getPermissionManagement() {
        return CloudNet.getInstance().getPermissionManagement();
    }

    private void sendUpdateToAllServices(IPacket packet) {
        for (ICloudService cloudService : CloudNet.getInstance().getCloudServiceManager().getCloudServices().values()) {
            if (cloudService.getNetworkChannel() != null) {
                cloudService.getNetworkChannel().sendPacket(packet);
            }
        }
    }
}