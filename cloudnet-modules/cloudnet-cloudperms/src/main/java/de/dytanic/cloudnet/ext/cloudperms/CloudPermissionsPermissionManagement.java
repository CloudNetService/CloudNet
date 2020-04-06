package de.dytanic.cloudnet.ext.cloudperms;


import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;

/**
 * @deprecated has been replaced with {@link CloudPermissionsManagement}, will be removed in a future release
 */
@Deprecated
public class CloudPermissionsPermissionManagement extends CloudPermissionsManagement {

    CloudPermissionsPermissionManagement(IPermissionManagement childPermissionManagement) {
        super(childPermissionManagement);
    }

    public static CloudPermissionsPermissionManagement getInstance() {
        IPermissionManagement permissionManagement = CloudNetDriver.getInstance().getPermissionManagement();
        Preconditions.checkArgument(permissionManagement instanceof CloudPermissionsPermissionManagement, "CloudPerms is not enabled");
        return (CloudPermissionsPermissionManagement) permissionManagement;
    }

}
