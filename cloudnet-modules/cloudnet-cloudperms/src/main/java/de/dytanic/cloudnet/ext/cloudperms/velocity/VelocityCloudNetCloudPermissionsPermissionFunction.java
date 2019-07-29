package de.dytanic.cloudnet.ext.cloudperms.velocity;

import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.Tristate;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsPermissionManagement;

import java.util.UUID;

public final class VelocityCloudNetCloudPermissionsPermissionFunction implements PermissionFunction {

    private final UUID uniqueId;

    public VelocityCloudNetCloudPermissionsPermissionFunction(UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    @Override
    public Tristate getPermissionValue(String permission) {
        if (permission == null) {
            return Tristate.FALSE;
        }

        IPermissionUser permissionUser = CloudPermissionsPermissionManagement.getInstance().getUser(uniqueId);
        return (permissionUser != null && CloudPermissionsPermissionManagement.getInstance().hasPlayerPermission(permissionUser, permission)) ?
                Tristate.TRUE : Tristate.FALSE;
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }
}