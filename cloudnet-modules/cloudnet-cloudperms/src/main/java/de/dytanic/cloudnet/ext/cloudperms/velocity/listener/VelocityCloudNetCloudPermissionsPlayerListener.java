package de.dytanic.cloudnet.ext.cloudperms.velocity.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsHelper;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsManagement;
import de.dytanic.cloudnet.ext.cloudperms.velocity.VelocityCloudNetCloudPermissionsPlugin;
import net.kyori.text.TextComponent;

public final class VelocityCloudNetCloudPermissionsPlayerListener {

    private final CloudPermissionsManagement permissionsManagement;

    public VelocityCloudNetCloudPermissionsPlayerListener(CloudPermissionsManagement permissionsManagement) {
        this.permissionsManagement = permissionsManagement;
    }

    @Subscribe(order = PostOrder.FIRST)
    public void handle(LoginEvent event) {
        CloudPermissionsHelper.initPermissionUser(this.permissionsManagement, event.getPlayer().getUniqueId(), event.getPlayer().getUsername(), message -> {
            event.setResult(ResultedEvent.ComponentResult.denied(TextComponent.of(message.replace("&", "§"))));
        });
    }

    @Subscribe
    public void handle(PermissionsSetupEvent event) {
        event.setProvider(VelocityCloudNetCloudPermissionsPlugin.getInstance().getPermissionProvider());
    }

    @Subscribe
    public void handle(DisconnectEvent event) {
        this.permissionsManagement.getCachedPermissionUsers().remove(event.getPlayer().getUniqueId());
    }
}