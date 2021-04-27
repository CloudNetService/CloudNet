package de.dytanic.cloudnet.wrapper.network.listener;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.event.events.permission.*;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.packet.PacketServerUpdatePermissions;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import de.dytanic.cloudnet.driver.permission.PermissionUser;

public final class PacketServerUpdatePermissionsListener implements IPacketListener {

    @Override
    public void handle(INetworkChannel channel, IPacket packet) {
        PacketServerUpdatePermissions.UpdateType updateType = packet.getBuffer().readEnumConstant(PacketServerUpdatePermissions.UpdateType.class);
        switch (updateType) {
            case ADD_USER:
                this.invoke0(new PermissionAddUserEvent(CloudNetDriver.getInstance().getPermissionManagement(), packet.getBuffer().readObject(PermissionUser.class)));
                break;
            case ADD_GROUP:
                this.invoke0(new PermissionAddGroupEvent(CloudNetDriver.getInstance().getPermissionManagement(), packet.getBuffer().readObject(PermissionGroup.class)));
                break;
            case SET_USERS:
                this.invoke0(new PermissionSetUsersEvent(CloudNetDriver.getInstance().getPermissionManagement(), packet.getBuffer().readObjectCollection(PermissionUser.class)));
                break;
            case SET_GROUPS:
                this.invoke0(new PermissionSetGroupsEvent(CloudNetDriver.getInstance().getPermissionManagement(), packet.getBuffer().readObjectCollection(PermissionGroup.class)));
                break;
            case DELETE_USER:
                this.invoke0(new PermissionDeleteUserEvent(CloudNetDriver.getInstance().getPermissionManagement(), packet.getBuffer().readObject(PermissionUser.class)));
                break;
            case UPDATE_USER:
                this.invoke0(new PermissionUpdateUserEvent(CloudNetDriver.getInstance().getPermissionManagement(), packet.getBuffer().readObject(PermissionUser.class)));
                break;
            case DELETE_GROUP:
                this.invoke0(new PermissionDeleteGroupEvent(CloudNetDriver.getInstance().getPermissionManagement(), packet.getBuffer().readObject(PermissionGroup.class)));
                break;
            case UPDATE_GROUP:
                this.invoke0(new PermissionUpdateGroupEvent(CloudNetDriver.getInstance().getPermissionManagement(), packet.getBuffer().readObject(PermissionGroup.class)));
                break;
        }
    }

    private void invoke0(Event event) {
        CloudNetDriver.getInstance().getEventManager().callEvent(event);
    }
}