package de.dytanic.cloudnet.ext.cloudperms.velocity.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsHelper;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsPermissionManagement;
import de.dytanic.cloudnet.ext.cloudperms.velocity.VelocityCloudNetCloudPermissionsPlugin;

public final class VelocityCloudNetCloudPermissionsPlayerListener {

    @Subscribe(order = PostOrder.FIRST)
    public void handle(LoginEvent event) {
        CloudPermissionsHelper.initPermissionUser(event.getPlayer().getUniqueId(), event.getPlayer().getUsername());
    }

    @Subscribe
    public void handle(PermissionsSetupEvent event) {
        event.setProvider(VelocityCloudNetCloudPermissionsPlugin.getInstance().getPermissionProvider());
    }

    @Subscribe
    public void handle(DisconnectEvent event) {
        CloudPermissionsPermissionManagement.getInstance().getCachedPermissionUsers().remove(event.getPlayer().getUniqueId());
    }
}