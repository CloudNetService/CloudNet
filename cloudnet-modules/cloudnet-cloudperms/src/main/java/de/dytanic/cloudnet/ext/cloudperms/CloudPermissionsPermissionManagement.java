package de.dytanic.cloudnet.ext.cloudperms;


/**
 * @deprecated has been replaced with {@link CloudPermissionsManagement}, will be removed in a future release
 */
@Deprecated
public class CloudPermissionsPermissionManagement extends CloudPermissionsManagement {
  
    public static CloudPermissionsPermissionManagement getInstance() {
        return (CloudPermissionsPermissionManagement) CloudPermissionsManagement.getInstance();
    }
  
}
