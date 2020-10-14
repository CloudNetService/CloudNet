package de.dytanic.cloudnet.network.listener.cluster;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.event.events.permission.*;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.packet.PacketServerUpdatePermissions;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.permission.*;
import de.dytanic.cloudnet.permission.ClusterSynchronizedPermissionManagement;
import de.dytanic.cloudnet.service.ICloudService;

import java.util.Collection;

public final class PacketServerUpdatePermissionsListener implements IPacketListener {

    @Override
    public void handle(INetworkChannel channel, IPacket packet) {
        packet.getBuffer().markReaderIndex();
        PacketServerUpdatePermissions.UpdateType updateType = packet.getBuffer().readEnumConstant(PacketServerUpdatePermissions.UpdateType.class);

        switch (updateType) {
            case ADD_USER: {
                IPermissionUser permissionUser = packet.getBuffer().readObject(PermissionUser.class);
                this.invoke0(new PermissionAddUserEvent(this.getPermissionManagement(), permissionUser));
                if (this.getPermissionManagement() instanceof ClusterSynchronizedPermissionManagement) {
                    ((ClusterSynchronizedPermissionManagement) this.getPermissionManagement()).addUserWithoutClusterSyncAsync(permissionUser);
                }
            }
            break;
            case ADD_GROUP: {
                IPermissionGroup permissionGroup = packet.getBuffer().readObject(PermissionGroup.class);
                this.invoke0(new PermissionAddGroupEvent(this.getPermissionManagement(), permissionGroup));
                if (this.getPermissionManagement() instanceof ClusterSynchronizedPermissionManagement) {
                    ((ClusterSynchronizedPermissionManagement) this.getPermissionManagement()).addGroupWithoutClusterSyncAsync(permissionGroup);
                }
            }
            break;
            case SET_USERS: {
                Collection<? extends IPermissionUser> permissionUsers = packet.getBuffer().readObjectCollection(PermissionUser.class);
                this.invoke0(new PermissionSetUsersEvent(this.getPermissionManagement(), permissionUsers));
                if (this.getPermissionManagement() instanceof ClusterSynchronizedPermissionManagement) {
                    ((ClusterSynchronizedPermissionManagement) this.getPermissionManagement()).setUsersWithoutClusterSyncAsync(permissionUsers);
                }
            }
            break;
            case SET_GROUPS: {
                Collection<? extends IPermissionGroup> permissionGroups = packet.getBuffer().readObjectCollection(PermissionGroup.class);
                this.invoke0(new PermissionSetGroupsEvent(this.getPermissionManagement(), permissionGroups));
                if (this.getPermissionManagement() instanceof ClusterSynchronizedPermissionManagement) {
                    ((ClusterSynchronizedPermissionManagement) this.getPermissionManagement()).setGroupsWithoutClusterSyncAsync(permissionGroups);
                }
            }
            break;
            case DELETE_USER: {
                IPermissionUser permissionUser = packet.getBuffer().readObject(PermissionUser.class);
                this.invoke0(new PermissionUpdateUserEvent(this.getPermissionManagement(), permissionUser));
                if (this.getPermissionManagement() instanceof ClusterSynchronizedPermissionManagement) {
                    ((ClusterSynchronizedPermissionManagement) this.getPermissionManagement()).deleteUserWithoutClusterSyncAsync(permissionUser);
                }
            }
            break;
            case UPDATE_USER: {
                IPermissionUser permissionUser = packet.getBuffer().readObject(PermissionUser.class);
                this.invoke0(new PermissionDeleteUserEvent(this.getPermissionManagement(), permissionUser));
                if (this.getPermissionManagement() instanceof ClusterSynchronizedPermissionManagement) {
                    ((ClusterSynchronizedPermissionManagement) this.getPermissionManagement()).updateUserWithoutClusterSyncAsync(permissionUser);
                }
            }
            break;
            case DELETE_GROUP: {
                IPermissionGroup permissionGroup = packet.getBuffer().readObject(PermissionGroup.class);
                this.invoke0(new PermissionDeleteGroupEvent(this.getPermissionManagement(), permissionGroup));
                if (this.getPermissionManagement() instanceof ClusterSynchronizedPermissionManagement) {
                    ((ClusterSynchronizedPermissionManagement) this.getPermissionManagement()).deleteGroupWithoutClusterSyncAsync(permissionGroup);
                }
            }
            break;
            case UPDATE_GROUP: {
                IPermissionGroup permissionGroup = packet.getBuffer().readObject(PermissionGroup.class);
                this.invoke0(new PermissionUpdateGroupEvent(this.getPermissionManagement(), permissionGroup));
                if (this.getPermissionManagement() instanceof ClusterSynchronizedPermissionManagement) {
                    ((ClusterSynchronizedPermissionManagement) this.getPermissionManagement()).updateGroupWithoutClusterSyncAsync(permissionGroup);
                }
            }
            break;
        }

        packet.getBuffer().resetReaderIndex();
        this.sendUpdateToAllServices(packet);
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