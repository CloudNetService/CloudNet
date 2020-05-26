package de.dytanic.cloudnet.ext.cloudperms.velocity;

import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.Tristate;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsManagement;

import java.util.UUID;

public final class VelocityCloudNetCloudPermissionsPermissionFunction implements PermissionFunction {

    private final UUID uniqueId;
    private final CloudPermissionsManagement permissionsManagement;

    public VelocityCloudNetCloudPermissionsPermissionFunction(UUID uniqueId, CloudPermissionsManagement permissionsManagement) {
        this.uniqueId = uniqueId;
        this.permissionsManagement = permissionsManagement;
    }

    @Override
    public Tristate getPermissionValue(String permission) {
        if (permission == null) {
            return Tristate.FALSE;
        }

        IPermissionUser permissionUser = this.permissionsManagement.getUser(this.uniqueId);
        return (permissionUser != null && this.permissionsManagement.hasPermission(permissionUser, permission)) ?
                Tristate.TRUE : Tristate.FALSE;
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }
}