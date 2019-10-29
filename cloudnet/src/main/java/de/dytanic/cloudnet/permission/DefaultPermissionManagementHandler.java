package de.dytanic.cloudnet.permission;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.concurrent.ITaskListener;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.events.permission.*;
import de.dytanic.cloudnet.driver.network.def.packet.PacketServerUpdatePermissions;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionManagementHandler;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;

import java.util.Collection;

public final class DefaultPermissionManagementHandler implements IPermissionManagementHandler {

    @Override
    public void handleAddUser(IPermissionManagement permissionManagement, IPermissionUser permissionUser) {
        CloudNetDriver.getInstance().getEventManager().callEvent(new PermissionAddUserEvent(permissionManagement, permissionUser));
        sendAll(new PacketServerUpdatePermissions(PacketServerUpdatePermissions.UpdateType.ADD_USER, permissionUser));
    }

    @Override
    public void handleUpdateUser(IPermissionManagement permissionManagement, IPermissionUser permissionUser) {
        CloudNetDriver.getInstance().getEventManager().callEvent(new PermissionUpdateUserEvent(permissionManagement, permissionUser));
        sendAll(new PacketServerUpdatePermissions(PacketServerUpdatePermissions.UpdateType.UPDATE_USER, permissionUser));
    }

    @Override
    public void handleDeleteUser(IPermissionManagement permissionManagement, IPermissionUser permissionUser) {
        CloudNetDriver.getInstance().getEventManager().callEvent(new PermissionDeleteUserEvent(permissionManagement, permissionUser));
        sendAll(new PacketServerUpdatePermissions(PacketServerUpdatePermissions.UpdateType.DELETE_USER, permissionUser));
    }

    @Override
    public void handleSetUsers(IPermissionManagement permissionManagement, Collection<? extends IPermissionUser> users) {
        CloudNetDriver.getInstance().getEventManager().callEvent(new PermissionSetUsersEvent(permissionManagement, users));
        sendAll(new PacketServerUpdatePermissions(PacketServerUpdatePermissions.UpdateType.SET_USERS, users));
    }

    @Override
    public void handleAddGroup(IPermissionManagement permissionManagement, IPermissionGroup permissionGroup) {
        CloudNetDriver.getInstance().getEventManager().callEvent(new PermissionAddGroupEvent(permissionManagement, permissionGroup));
        sendAll(new PacketServerUpdatePermissions(PacketServerUpdatePermissions.UpdateType.ADD_GROUP, permissionGroup));
    }

    @Override
    public void handleUpdateGroup(IPermissionManagement permissionManagement, IPermissionGroup permissionGroup) {
        CloudNetDriver.getInstance().getEventManager().callEvent(new PermissionUpdateGroupEvent(permissionManagement, permissionGroup));
        sendAll(new PacketServerUpdatePermissions(PacketServerUpdatePermissions.UpdateType.UPDATE_GROUP, permissionGroup));
    }

    @Override
    public void handleDeleteGroup(IPermissionManagement permissionManagement, IPermissionGroup permissionGroup) {
        CloudNetDriver.getInstance().getEventManager().callEvent(new PermissionDeleteGroupEvent(permissionManagement, permissionGroup));
        sendAll(new PacketServerUpdatePermissions(PacketServerUpdatePermissions.UpdateType.DELETE_GROUP, permissionGroup));
    }

    @Override
    public void handleSetGroups(IPermissionManagement permissionManagement, Collection<? extends IPermissionGroup> groups) {
        CloudNetDriver.getInstance().getEventManager().callEvent(new PermissionSetGroupsEvent(permissionManagement, groups));
        sendAll(new PacketServerUpdatePermissions(PacketServerUpdatePermissions.UpdateType.SET_GROUPS, groups));
    }

    @Override
    public void handleReloaded(IPermissionManagement permissionManagement) {
        sendAll(new PacketServerUpdatePermissions(PacketServerUpdatePermissions.UpdateType.SET_GROUPS, permissionManagement.getGroups()));
    }

    private void sendAll(IPacket packet) {
        CloudNet.getInstance().sendAllAsync(packet).addListener(ITaskListener.FIRE_EXCEPTION_ON_FAILURE);
    }
}