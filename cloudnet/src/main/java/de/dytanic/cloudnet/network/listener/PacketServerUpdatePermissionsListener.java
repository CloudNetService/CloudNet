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
                    invoke0(new PermissionAddUserEvent(getPermissionManagement(), permissionUser));
                    if (getPermissionManagement() instanceof ClusterSynchronizedPermissionManagement) {
                        ((ClusterSynchronizedPermissionManagement) getPermissionManagement()).addUserWithoutClusterSync(permissionUser);
                    }
                }
                break;
                case ADD_GROUP: {
                    IPermissionGroup permissionGroup = packet.getHeader().get("permissionGroup", PermissionGroup.TYPE);
                    invoke0(new PermissionAddGroupEvent(getPermissionManagement(), permissionGroup));
                    if (getPermissionManagement() instanceof ClusterSynchronizedPermissionManagement) {
                        ((ClusterSynchronizedPermissionManagement) getPermissionManagement()).addGroupWithoutClusterSync(permissionGroup);
                    }
                }
                break;
                case SET_USERS: {
                    Collection<? extends IPermissionUser> permissionUsers = packet.getHeader().get("permissionUsers", PERMISSION_USERS_TYPE);
                    invoke0(new PermissionSetUsersEvent(getPermissionManagement(), permissionUsers));
                    if (getPermissionManagement() instanceof ClusterSynchronizedPermissionManagement) {
                        ((ClusterSynchronizedPermissionManagement) getPermissionManagement()).setUsersWithoutClusterSync(permissionUsers);
                    }
                }
                break;
                case SET_GROUPS: {
                    Collection<? extends IPermissionGroup> permissionGroups = packet.getHeader().get("permissionGroups", PERMISSION_GROUPS_TYPE);
                    invoke0(new PermissionSetGroupsEvent(getPermissionManagement(), permissionGroups));
                    if (getPermissionManagement() instanceof ClusterSynchronizedPermissionManagement) {
                        ((ClusterSynchronizedPermissionManagement) getPermissionManagement()).setGroupsWithoutClusterSync(permissionGroups);
                    }
                }
                break;
                case DELETE_USER: {
                    IPermissionUser permissionUser = packet.getHeader().get("permissionUser", PermissionUser.TYPE);
                    invoke0(new PermissionUpdateUserEvent(getPermissionManagement(), permissionUser));
                    if (getPermissionManagement() instanceof ClusterSynchronizedPermissionManagement) {
                        ((ClusterSynchronizedPermissionManagement) getPermissionManagement()).deleteUserWithoutClusterSync(permissionUser);
                    }
                }
                break;
                case UPDATE_USER: {
                    IPermissionUser permissionUser = packet.getHeader().get("permissionUser", PermissionUser.TYPE);
                    invoke0(new PermissionDeleteUserEvent(getPermissionManagement(), permissionUser));
                    if (getPermissionManagement() instanceof ClusterSynchronizedPermissionManagement) {
                        ((ClusterSynchronizedPermissionManagement) getPermissionManagement()).updateUserWithoutClusterSync(permissionUser);
                    }
                }
                break;
                case DELETE_GROUP: {
                    IPermissionGroup permissionGroup = packet.getHeader().get("permissionGroup", PermissionGroup.TYPE);
                    invoke0(new PermissionDeleteGroupEvent(getPermissionManagement(), permissionGroup));
                    if (getPermissionManagement() instanceof ClusterSynchronizedPermissionManagement) {
                        ((ClusterSynchronizedPermissionManagement) getPermissionManagement()).deleteGroupWithoutClusterSync(permissionGroup);
                    }
                }
                break;
                case UPDATE_GROUP: {
                    IPermissionGroup permissionGroup = packet.getHeader().get("permissionGroup", PermissionGroup.TYPE);
                    invoke0(new PermissionUpdateGroupEvent(getPermissionManagement(), permissionGroup));
                    if (getPermissionManagement() instanceof ClusterSynchronizedPermissionManagement) {
                        ((ClusterSynchronizedPermissionManagement) getPermissionManagement()).updateGroupWithoutClusterSync(permissionGroup);
                    }
                }
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