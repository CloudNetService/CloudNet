package de.dytanic.cloudnet.ext.cloudperms.gomint;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.driver.permission.Permission;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsManagement;
import io.gomint.entity.EntityPlayer;
import io.gomint.permission.Group;
import io.gomint.server.permission.PermissionManager;

public final class GoMintCloudNetCloudPermissionsPermissionManager extends PermissionManager {

    private final EntityPlayer player;

    private final io.gomint.permission.PermissionManager defaultPermissionManager;

    public GoMintCloudNetCloudPermissionsPermissionManager(io.gomint.server.entity.EntityPlayer player, io.gomint.permission.PermissionManager permissionManager) {
        super(player);

        this.player = player;
        this.defaultPermissionManager = permissionManager;
    }

    @Override
    public boolean hasPermission(String s) {
        if (s == null) {
            return false;
        }

        IPermissionUser permissionUser = getUser();
        return permissionUser != null && CloudPermissionsManagement.getInstance().hasPlayerPermission(permissionUser, s);
    }

    @Override
    public void addGroup(Group group) {
        if (defaultPermissionManager != null) {
            defaultPermissionManager.addGroup(group);
        }
    }

    @Override
    public void removeGroup(Group group) {
        if (defaultPermissionManager != null) {
            defaultPermissionManager.removeGroup(group);
        }
    }

    @Override
    public void setPermission(String s, boolean b) {
        Validate.checkNotNull(s);

        IPermissionUser permissionUser = getUser();
        permissionUser.addPermission(new Permission(s, b ? 1 : -1));
        CloudPermissionsManagement.getInstance().updateUser(permissionUser);
    }

    @Override
    public void removePermission(String s) {
        Validate.checkNotNull(s);

        IPermissionUser permissionUser = getUser();
        permissionUser.removePermission(s);
        CloudPermissionsManagement.getInstance().updateUser(permissionUser);
    }


    private IPermissionUser getUser() {
        return CloudPermissionsManagement.getInstance().getUser(player.getUUID());
    }

    public EntityPlayer getPlayer() {
        return this.player;
    }

    public io.gomint.permission.PermissionManager getDefaultPermissionManager() {
        return this.defaultPermissionManager;
    }
}