package de.dytanic.cloudnet.ext.cloudperms.velocity.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsPermissionManagement;
import de.dytanic.cloudnet.ext.cloudperms.velocity.VelocityCloudNetCloudPermissionsPlugin;

public final class VelocityCloudNetCloudPermissionsPlayerListener {

    @Subscribe(order = PostOrder.FIRST)
    public void handle(LoginEvent event)
    {
        IPermissionUser permissionUser = CloudPermissionsPermissionManagement.getInstance().getUser(event.getPlayer().getUniqueId());

        if (permissionUser == null)
        {
            CloudPermissionsPermissionManagement.getInstance().addUser(new PermissionUser(
                event.getPlayer().getUniqueId(),
                event.getPlayer().getUsername(),
                null,
                0
            ));

            permissionUser = CloudPermissionsPermissionManagement.getInstance().getUser(event.getPlayer().getUniqueId());
        }

        if (permissionUser != null)
        {
            CloudPermissionsPermissionManagement.getInstance().getCachedPermissionUsers().put(permissionUser.getUniqueId(), permissionUser);
            permissionUser.setName(event.getPlayer().getUsername());
            CloudPermissionsPermissionManagement.getInstance().updateUser(permissionUser);
        }
    }

    @Subscribe
    public void handle(PermissionsSetupEvent event)
    {
        event.setProvider(VelocityCloudNetCloudPermissionsPlugin.getInstance().getPermissionProvider());
    }

    @Subscribe
    public void handle(DisconnectEvent event)
    {
        CloudPermissionsPermissionManagement.getInstance().getCachedPermissionUsers().remove(event.getPlayer().getUniqueId());
    }
}