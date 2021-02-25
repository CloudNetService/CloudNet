package de.dytanic.cloudnet.ext.cloudperms.gomint;

import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.driver.permission.Permission;
import io.gomint.entity.EntityPlayer;
import io.gomint.permission.Group;
import io.gomint.permission.PermissionManager;

public final class GoMintCloudNetCloudPermissionsPermissionManager implements PermissionManager {

    private final EntityPlayer player;

    private final IPermissionManagement permissionManagement;

    public GoMintCloudNetCloudPermissionsPermissionManager(EntityPlayer player, IPermissionManagement permissionManagement) {
        this.player = player;
        this.permissionManagement = permissionManagement;
    }

    @Override
    public boolean has(String permission) {
        if (permission == null) {
            return false;
        }

        IPermissionUser permissionUser = this.getUser();
        return permissionUser != null && this.permissionManagement.hasPermission(permissionUser, permission);
    }

    @Override
    public boolean has(String permission, boolean defaultValue) {
        return this.has(permission) || defaultValue;
    }

    @Override
    public PermissionManager addGroup(Group group) {
        return this;
    }

    @Override
    public PermissionManager removeGroup(Group group) {
        return this;
    }

    @Override
    public PermissionManager permission(String permission, boolean value) {
        IPermissionUser permissionUser = this.getUser();
        permissionUser.addPermission(new Permission(permission, value ? 1 : -1));
        this.permissionManagement.updateUser(permissionUser);

        return this;
    }

    @Override
    public PermissionManager remove(String permission) {
        IPermissionUser permissionUser = this.getUser();
        permissionUser.removePermission(permission);
        this.permissionManagement.updateUser(permissionUser);

        return this;
    }

    @Override
    public PermissionManager toggleOp() {
        return this;
    }

    private IPermissionUser getUser() {
        return this.permissionManagement.getUser(this.player.uuid());
    }

    public EntityPlayer getPlayer() {
        return this.player;
    }

}