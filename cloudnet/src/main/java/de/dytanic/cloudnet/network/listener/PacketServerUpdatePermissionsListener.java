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
import de.dytanic.cloudnet.driver.permission.*;
import de.dytanic.cloudnet.permission.ClusterSynchronizedPermissionManagement;
import de.dytanic.cloudnet.service.ICloudService;

import java.lang.reflect.Type;
import java.util.Collection;

public final class PacketServerUpdatePermissionsListener implements IPacketListener {

    private static final Type PERMISSION_USERS_TYPE = new TypeToken<Collection<PermissionUser>>() {
    }.getType(), PERMISSION_GROUPS_TYPE = new TypeToken<Collection<PermissionGroup>>() {
    }.getType();

    @Override
    public void handle(INetworkChannel channel, IPacket packet) {
        if (packet.getHeader().contains("permissions_event") && packet.getHeader().contains("updateType")) {
            switch (packet.getHeader().get("updateType", PacketServerUpdatePermissions.UpdateType.class)) {
                case ADD_USER: {
                    IPermissionUser permissionUser = packet.getHeader().get("permissionUser", PermissionUser.TYPE);
                    this.invoke0(new PermissionAddUserEvent(this.getPermissionManagement(), permissionUser));
                    if (this.getPermissionManagement() instanceof ClusterSynchronizedPermissionManagement) {
                        ((ClusterSynchronizedPermissionManagement) this.getPermissionManagement()).addUserWithoutClusterSyncAsync(permissionUser);
                    }
                }
                break;
                case ADD_GROUP: {
                    IPermissionGroup permissionGroup = packet.getHeader().get("permissionGroup", PermissionGroup.TYPE);
                    this.invoke0(new PermissionAddGroupEvent(this.getPermissionManagement(), permissionGroup));
                    if (this.getPermissionManagement() instanceof ClusterSynchronizedPermissionManagement) {
                        ((ClusterSynchronizedPermissionManagement) this.getPermissionManagement()).addGroupWithoutClusterSyncAsync(permissionGroup);
                    }
                }
                break;
                case SET_USERS: {
                    Collection<? extends IPermissionUser> permissionUsers = packet.getHeader().get("permissionUsers", PERMISSION_USERS_TYPE);
                    this.invoke0(new PermissionSetUsersEvent(this.getPermissionManagement(), permissionUsers));
                    if (this.getPermissionManagement() instanceof ClusterSynchronizedPermissionManagement) {
                        ((ClusterSynchronizedPermissionManagement) this.getPermissionManagement()).setUsersWithoutClusterSyncAsync(permissionUsers);
                    }
                }
                break;
                case SET_GROUPS: {
                    Collection<? extends IPermissionGroup> permissionGroups = packet.getHeader().get("permissionGroups", PERMISSION_GROUPS_TYPE);
                    this.invoke0(new PermissionSetGroupsEvent(this.getPermissionManagement(), permissionGroups));
                    if (this.getPermissionManagement() instanceof ClusterSynchronizedPermissionManagement) {
                        ((ClusterSynchronizedPermissionManagement) this.getPermissionManagement()).setGroupsWithoutClusterSyncAsync(permissionGroups);
                    }
                }
                break;
                case DELETE_USER: {
                    IPermissionUser permissionUser = packet.getHeader().get("permissionUser", PermissionUser.TYPE);
                    this.invoke0(new PermissionUpdateUserEvent(this.getPermissionManagement(), permissionUser));
                    if (this.getPermissionManagement() instanceof ClusterSynchronizedPermissionManagement) {
                        ((ClusterSynchronizedPermissionManagement) this.getPermissionManagement()).deleteUserWithoutClusterSyncAsync(permissionUser);
                    }
                }
                break;
                case UPDATE_USER: {
                    IPermissionUser permissionUser = packet.getHeader().get("permissionUser", PermissionUser.TYPE);
                    this.invoke0(new PermissionDeleteUserEvent(this.getPermissionManagement(), permissionUser));
                    if (this.getPermissionManagement() instanceof ClusterSynchronizedPermissionManagement) {
                        ((ClusterSynchronizedPermissionManagement) this.getPermissionManagement()).updateUserWithoutClusterSyncAsync(permissionUser);
                    }
                }
                break;
                case DELETE_GROUP: {
                    IPermissionGroup permissionGroup = packet.getHeader().get("permissionGroup", PermissionGroup.TYPE);
                    this.invoke0(new PermissionDeleteGroupEvent(this.getPermissionManagement(), permissionGroup));
                    if (this.getPermissionManagement() instanceof ClusterSynchronizedPermissionManagement) {
                        ((ClusterSynchronizedPermissionManagement) this.getPermissionManagement()).deleteGroupWithoutClusterSyncAsync(permissionGroup);
                    }
                }
                break;
                case UPDATE_GROUP: {
                    IPermissionGroup permissionGroup = packet.getHeader().get("permissionGroup", PermissionGroup.TYPE);
                    this.invoke0(new PermissionUpdateGroupEvent(this.getPermissionManagement(), permissionGroup));
                    if (this.getPermissionManagement() instanceof ClusterSynchronizedPermissionManagement) {
                        ((ClusterSynchronizedPermissionManagement) this.getPermissionManagement()).updateGroupWithoutClusterSyncAsync(permissionGroup);
                    }
                }
                break;
            }

            this.sendUpdateToAllServices(packet);
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